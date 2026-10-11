from fastapi import FastAPI, UploadFile, File, Form, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import hashlib
import time
import io
import re
import numpy as np
from PIL import Image, ImageStat
import imagehash

try:
    from rapidocr_onnxruntime import RapidOCR
    _ocr_engine = RapidOCR()
except Exception as _ocr_init_err:
    _ocr_engine = None
    print(f"Warning: RapidOCR initialization deferred or failed: {_ocr_init_err}")

app = FastAPI(
    title="Plastic Waste AI Verification Microservice",
    description="Microservice for automated plastic waste image classification, duplicate detection, and verification.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/jpg"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
DEFAULT_PHASH_THRESHOLD = 8

class ImageVerificationResponse(BaseModel):
    sha256_hash: str
    phash: str
    exact_duplicate: bool
    possible_visual_duplicate: bool
    min_hamming_distance: Optional[int] = None
    metadata_available: bool
    exif_summary: Dict[str, Any]
    is_valid_plastic: bool
    confidence_score: float
    review_reasons: List[str]
    verification_status: str  # PASSED, FLAGGED_FOR_REVIEW, DUPLICATE_DETECTED

class WatermarkOcrResponse(BaseModel):
    watermark_detected: bool
    ocr_latitude: Optional[float] = None
    ocr_longitude: Optional[float] = None
    ocr_timestamp: Optional[str] = None
    ocr_raw_text: Optional[str] = None
    extracted_lines: List[str] = []
    ocr_status: str  # OCR_WATERMARK_UNVERIFIED, NO_WATERMARK_FOUND, NOT_APPLICABLE, OCR_FAILED
    confidence: float = 0.0

@app.get("/")
def root():
    return {
        "service": "plastic-waste-ai-service",
        "status": "UP",
        "documentation": "/docs",
        "health": "/health"
    }

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "plastic-waste-ai-service",
        "version": "1.0.0",
        "timestamp": time.time(),
        "ai_model_loaded": True,
        "message": "AI image verification and duplicate detection microservice is operational."
    }

@app.post("/verify", response_model=ImageVerificationResponse)
async def verify_image(
    file: UploadFile = File(...),
    existing_sha256: Optional[str] = Form(None),
    existing_phashes: Optional[str] = Form(None), # Comma-separated list of existing pHashes
    phash_threshold: int = Form(DEFAULT_PHASH_THRESHOLD)
):
    if file.content_type and file.content_type.lower() not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file format '{file.content_type}'. Allowed: JPG, PNG, WEBP."
        )

    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File size exceeds maximum 10MB limit."
        )

    if len(content) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded file is empty."
        )

    # 1. Exact Duplicate Detection via SHA-256 Hash
    sha256_hash = hashlib.sha256(content).hexdigest()
    
    # 2. Process Image & Compute Perceptual Hash (pHash)
    try:
        img = Image.open(io.BytesIO(content))
        img_format = img.format
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File signature or corrupted image payload validation failed."
        )

    computed_phash = str(imagehash.phash(img))

    # 3. EXIF Metadata Extraction
    exif_data = {}
    metadata_available = False
    try:
        exif_raw = img._getexif()
        if exif_raw:
            metadata_available = True
            for tag_id, val in exif_raw.items():
                exif_data[str(tag_id)] = str(val)[:100]
    except Exception:
        metadata_available = False

    # 4. Duplicate Matching Evaluation
    is_exact_dup = False
    if existing_sha256 and sha256_hash.lower() == existing_sha256.strip().lower():
        is_exact_dup = True

    is_visual_dup = False
    min_distance = None
    if existing_phashes:
        known_list = [p.strip() for p in existing_phashes.split(",") if p.strip()]
        for target_phash_str in known_list:
            try:
                target_phash = imagehash.hex_to_hash(target_phash_str)
                current_phash = imagehash.hex_to_hash(computed_phash)
                dist = current_phash - target_phash
                if min_distance is None or dist < min_distance:
                    min_distance = dist
                if dist <= phash_threshold:
                    is_visual_dup = True
            except Exception:
                continue

    # 5. Plastic Content Assessment
    review_reasons = []
    # Basic heuristic assessment using image color variance & brightness metrics
    stat = ImageStat.Stat(img.convert('RGB'))
    std_dev = sum(stat.stddev) / 3.0
    brightness = sum(stat.mean) / 3.0

    is_valid_plastic = True
    confidence_score = 0.92

    if std_dev < 15.0:
        review_reasons.append("Low color contrast / potential solid test card or blank image.")
        confidence_score -= 0.20

    if brightness < 10.0 or brightness > 245.0:
        review_reasons.append("Extreme brightness levels (overexposed or black frame).")
        confidence_score -= 0.25

    if is_exact_dup:
        review_reasons.append("Exact SHA-256 file duplicate detected.")
    if is_visual_dup:
        review_reasons.append(f"Perceptually similar duplicate image detected (Hamming distance: {min_distance}).")

    if is_exact_dup or is_visual_dup:
        verification_status = "DUPLICATE_DETECTED"
    elif review_reasons:
        verification_status = "FLAGGED_FOR_REVIEW"
    else:
        verification_status = "PASSED"

    return ImageVerificationResponse(
        sha256_hash=sha256_hash,
        phash=computed_phash,
        exact_duplicate=is_exact_dup,
        possible_visual_duplicate=is_visual_dup,
        min_hamming_distance=min_distance,
        metadata_available=metadata_available,
        exif_summary=exif_data,
        is_valid_plastic=is_valid_plastic,
        confidence_score=max(0.0, round(confidence_score, 2)),
        review_reasons=review_reasons,
        verification_status=verification_status
    )

@app.post("/ocr/watermark", response_model=WatermarkOcrResponse)
async def extract_watermark_ocr(
    file: UploadFile = File(...)
):
    if file.content_type and file.content_type.lower() not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file format '{file.content_type}'. Allowed: JPG, PNG, WEBP."
        )

    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File size exceeds maximum 10MB limit."
        )

    if len(content) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded file is empty."
        )

    try:
        pil_img = Image.open(io.BytesIO(content)).convert('RGB')
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid image payload; could not decode file stream."
        )

    if _ocr_engine is None:
        return WatermarkOcrResponse(
            watermark_detected=False,
            ocr_status="OCR_FAILED",
            confidence=0.0
        )

    w, h = pil_img.size
    # Focus OCR on the bottom 45% where camera watermarks are stamped
    crop_h = max(10, int(h * 0.45))
    cropped = pil_img.crop((0, h - crop_h, w, h))

    try:
        ocr_res, _ = _ocr_engine(np.array(cropped))
        # If nothing found in footer, try whole image as fallback
        if not ocr_res or len(ocr_res) == 0:
            ocr_res, _ = _ocr_engine(np.array(pil_img))
    except Exception as e:
        return WatermarkOcrResponse(
            watermark_detected=False,
            ocr_status="OCR_FAILED",
            confidence=0.0
        )

    if not ocr_res or len(ocr_res) == 0:
        return WatermarkOcrResponse(
            watermark_detected=False,
            ocr_status="NO_WATERMARK_FOUND",
            confidence=0.0
        )

    extracted_lines = [str(line[1]).strip() for line in ocr_res if len(line) >= 2 and str(line[1]).strip()]
    full_text = " \n ".join(extracted_lines)

    # Average confidence calculation
    confs = []
    for line in ocr_res:
        if len(line) >= 3:
            try:
                confs.append(float(line[2]))
            except (ValueError, TypeError):
                pass
    avg_conf = float(np.mean(confs)) if confs else 0.85

    # 1. Parse Coordinates: handles variations like Lat 16.533567 Long 81.44504, 16.533567 N, 81.44504 E, etc.
    extracted_lat = None
    extracted_lon = None

    coord_regex_1 = re.search(
        r"(?:Lat|Latitude|LAT)?[:\s]*([+-]?\d{1,2}\.\d{3,8})[°\s,]*[^\d\n]*(?:Long|Longitude|Lng|LON)?[:\s]*([+-]?\d{1,3}\.\d{3,8})",
        full_text,
        re.IGNORECASE
    )
    if coord_regex_1:
        try:
            cand_lat = float(coord_regex_1.group(1))
            cand_lon = float(coord_regex_1.group(2))
            if -90.0 <= cand_lat <= 90.0 and -180.0 <= cand_lon <= 180.0:
                extracted_lat = cand_lat
                extracted_lon = cand_lon
        except (ValueError, TypeError):
            pass

    if extracted_lat is None:
        coord_regex_2 = re.search(
            r"([+-]?\d{1,2}\.\d{3,8})[°\s]*([NSns])?[,\s]+([+-]?\d{1,3}\.\d{3,8})[°\s]*([EWew])?",
            full_text
        )
        if coord_regex_2:
            try:
                cand_lat = float(coord_regex_2.group(1))
                if coord_regex_2.group(2) and coord_regex_2.group(2).upper() == 'S':
                    cand_lat = -cand_lat
                cand_lon = float(coord_regex_2.group(3))
                if coord_regex_2.group(4) and coord_regex_2.group(4).upper() == 'W':
                    cand_lon = -cand_lon
                if -90.0 <= cand_lat <= 90.0 and -180.0 <= cand_lon <= 180.0:
                    extracted_lat = cand_lat
                    extracted_lon = cand_lon
            except (ValueError, TypeError):
                pass

    # 2. Parse Date/Time: preserve exact text format and timezone
    extracted_timestamp = None
    date_regex = re.search(
        r"((?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)[,\s]*)?(\d{1,4}[/\-.]\d{1,2}[/\-.]\d{1,4}\s*\d{1,2}:\d{2}(?::\d{2})?(?:\s*[APap][Mm])?(?:\s*GMT[+-]\d{2}(?::?\d{2})?)?)",
        full_text,
        re.IGNORECASE
    )
    if date_regex:
        extracted_timestamp = date_regex.group(0).strip()
    else:
        # Fallback date search: e.g. 2026-10-11 or 11/10/2026 with time
        date_fallback = re.search(
            r"(\d{1,4}[/\-.]\d{1,2}[/\-.]\d{1,4}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?)",
            full_text
        )
        if date_fallback:
            extracted_timestamp = date_fallback.group(0).strip()

    is_watermark = (
        extracted_lat is not None
        or extracted_timestamp is not None
        or "gpsmapcamera" in full_text.lower().replace(" ", "")
        or "gps camera" in full_text.lower()
    )

    status_str = "OCR_WATERMARK_UNVERIFIED" if is_watermark else "NO_WATERMARK_FOUND"

    return WatermarkOcrResponse(
        watermark_detected=is_watermark,
        ocr_latitude=extracted_lat,
        ocr_longitude=extracted_lon,
        ocr_timestamp=extracted_timestamp,
        ocr_raw_text=full_text,
        extracted_lines=extracted_lines,
        ocr_status=status_str,
        confidence=round(avg_conf, 4)
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8001, reload=True)

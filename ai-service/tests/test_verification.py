from fastapi.testclient import TestClient
from app.main import app
import io
from PIL import Image
import hashlib
import imagehash

client = TestClient(app)

def create_dummy_image(color=(100, 150, 200), size=(200, 200)):
    buf = io.BytesIO()
    img = Image.new("RGB", size, color=color)
    img.save(buf, format="JPEG")
    return buf.getvalue()

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"

def test_exact_duplicate_detection():
    img_bytes = create_dummy_image((120, 180, 80))
    expected_sha256 = hashlib.sha256(img_bytes).hexdigest()

    # First request: upload image
    res1 = client.post(
        "/verify",
        files={"file": ("sample.jpg", img_bytes, "image/jpeg")}
    )
    assert res1.status_code == 200
    data1 = res1.json()
    assert data1["sha256_hash"] == expected_sha256
    assert data1["exact_duplicate"] is False

    # Second request: upload same image and pass previous sha256
    res2 = client.post(
        "/verify",
        files={"file": ("sample.jpg", img_bytes, "image/jpeg")},
        data={"existing_sha256": expected_sha256}
    )
    assert res2.status_code == 200
    data2 = res2.json()
    assert data2["exact_duplicate"] is True
    assert data2["verification_status"] == "DUPLICATE_DETECTED"
    assert "Exact SHA-256 file duplicate detected." in data2["review_reasons"]

def test_visual_perceptual_duplicate_detection():
    img1_bytes = create_dummy_image((200, 100, 50))
    # Create slightly altered visual copy
    img2_bytes = create_dummy_image((202, 102, 52))

    img1 = Image.open(io.BytesIO(img1_bytes))
    phash1 = str(imagehash.phash(img1))

    res = client.post(
        "/verify",
        files={"file": ("sample2.jpg", img2_bytes, "image/jpeg")},
        data={"existing_phashes": phash1, "phash_threshold": "10"}
    )
    assert res.status_code == 200
    data = res.json()
    assert data["possible_visual_duplicate"] is True
    assert data["verification_status"] == "DUPLICATE_DETECTED"

def test_invalid_file_type():
    res = client.post(
        "/verify",
        files={"file": ("test.txt", b"plain text payload", "text/plain")}
    )
    assert res.status_code == 400
    assert "Unsupported file format" in res.json()["detail"]

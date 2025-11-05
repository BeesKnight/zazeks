from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import numpy as np
import cv2
from classifier import classify_gesture

app = FastAPI(title="zazeks-gesture-api", version="1.0")

@app.get("/health")
def health():
    return {"ok": True}

@app.post("/model/detect")
async def detect(file: UploadFile = File(...)):
    if not file:
        raise HTTPException(400, "no file")
    data = await file.read()
    if not data:
        raise HTTPException(400, "empty file")

    arr = np.frombuffer(data, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(400, "cannot decode image")

    result = classify_gesture(img)
    return JSONResponse(result)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)

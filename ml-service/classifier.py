import cv2
import numpy as np
import mediapipe as mp
from typing import Tuple, Dict, Any

mp_hands = mp.solutions.hands

def _to_rgb(img_bgr: np.ndarray) -> np.ndarray:
    return cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

def _bbox_from_landmarks(landmarks, w: int, h: int) -> Tuple[int, int, int, int]:
    xs = [int(min(max(lm.x * w, 0), w - 1)) for lm in landmarks]
    ys = [int(min(max(lm.y * h, 0), h - 1)) for lm in landmarks]
    x1, y1 = max(min(xs), 0), max(min(ys), 0)
    x2, y2 = min(max(xs), w - 1), min(max(ys), h - 1)
    return x1, y1, x2, y2

def _count_extended_fingers(landmarks, handedness: str) -> Tuple[int, Dict[str, bool]]:
    WRIST = 0
    TH_TIP, TH_IP = 4, 3
    ID_TIP, ID_PIP, ID_MCP = 8, 6, 5
    MD_TIP, MD_PIP, MD_MCP = 12, 10, 9
    RG_TIP, RG_PIP, RG_MCP = 16, 14, 13
    PK_TIP, PK_PIP, PK_MCP = 20, 18, 17
    lm = landmarks

    def up(tip, pip, mcp) -> bool:
        return lm[tip].y < lm[pip].y < lm[mcp].y

    idx = up(ID_TIP, ID_PIP, ID_MCP)
    mid = up(MD_TIP, MD_PIP, MD_MCP)
    rng = up(RG_TIP, RG_PIP, RG_MCP)
    pnk = up(PK_TIP, PK_PIP, PK_MCP)

    if handedness.lower().startswith("right"):
        thumb = lm[TH_TIP].x > lm[TH_IP].x
    else:
        thumb = lm[TH_TIP].x < lm[TH_IP].x

    status = {"thumb": thumb, "index": idx, "middle": mid, "ring": rng, "pinky": pnk}
    return sum(status.values()), status

def classify_gesture(img_bgr: np.ndarray) -> Dict[str, Any]:
    h, w = img_bgr.shape[:2]
    with mp_hands.Hands(static_image_mode=True, max_num_hands=1, min_detection_confidence=0.4) as hands:
        res = hands.process(_to_rgb(img_bgr))

    if not res.multi_hand_landmarks:
        return {"gesture": "Unknown", "bbox": [0, 0, w, h], "confidence": 0.0}

    landmarks = res.multi_hand_landmarks[0].landmark
    handedness = res.multi_handedness[0].classification[0].label
    x1, y1, x2, y2 = _bbox_from_landmarks(landmarks, w, h)

    count, status = _count_extended_fingers(landmarks, handedness)

    gesture, conf = "Unknown", 0.2
    if count == 0:
        gesture, conf = "Rock", 0.95
    elif count == 5:
        gesture, conf = "Paper", 0.95
    elif count == 2 and status["index"] and status["middle"] and not status["ring"] and not status["pinky"]:
        gesture, conf = "Scissors", 0.90

    return {"gesture": gesture, "bbox": [int(x1), int(y1), int(x2), int(y2)], "confidence": float(conf)}

import urllib.request
import json

base = "http://127.0.0.1:5001"

def test_get(path):
    try:
        resp = urllib.request.urlopen(base + path)
        print(f"GET {path} -> {resp.status}")
        data = resp.read().decode("utf-8")
        print(data[:500])
        print("---")
        return data
    except Exception as e:
        print(f"GET {path} -> ERROR: {e}")
        return None

def test_post(path, body):
    try:
        req = urllib.request.Request(
            base + path,
            data=json.dumps(body).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        resp = urllib.request.urlopen(req)
        print(f"POST {path} -> {resp.status}")
        data = resp.read().decode("utf-8")
        print(data[:500])
        print("---")
        return data
    except Exception as e:
        print(f"POST {path} -> ERROR: {e}")
        return None

def test_put(path, body=None):
    try:
        data_bytes = json.dumps(body).encode("utf-8") if body else b""
        req = urllib.request.Request(
            base + path,
            data=data_bytes,
            headers={"Content-Type": "application/json"} if body else {},
            method="PUT"
        )
        resp = urllib.request.urlopen(req)
        print(f"PUT {path} -> {resp.status}")
        data = resp.read().decode("utf-8")
        print(data[:500])
        print("---")
        return data
    except Exception as e:
        print(f"PUT {path} -> ERROR: {e}")
        return None

def test_delete(path):
    try:
        req = urllib.request.Request(base + path, method="DELETE")
        resp = urllib.request.urlopen(req)
        print(f"DELETE {path} -> {resp.status}")
        data = resp.read().decode("utf-8")
        print(data[:500])
        print("---")
        return data
    except Exception as e:
        print(f"DELETE {path} -> ERROR: {e}")
        return None

print("=" * 50)
print("测试香薰蜡烛 API")
print("=" * 50)

# 1. 获取初始列表
print("\n[1] 获取蜡烛列表（初始）")
test_get("/api/candles")

# 2. 获取统计
print("\n[2] 获取统计数据")
test_get("/api/stats")

# 3. 新增蜡烛
print("\n[3] 新增蜡烛")
resp = test_post("/api/candles", {
    "name": "冬日森林",
    "scent": "雪松琥珀",
    "capacity": 220,
    "remaining_ratio": 100,
    "note": "圣诞礼物，超喜欢！"
})
candle1 = json.loads(resp) if resp else None

# 4. 再新增一支快用完的
print("\n[4] 新增一支快用完的蜡烛")
resp2 = test_post("/api/candles", {
    "name": "玫瑰花园",
    "scent": "玫瑰天竺葵",
    "capacity": 180,
    "remaining_ratio": 10,
    "note": "快用完了"
})
candle2 = json.loads(resp2) if resp2 else None

# 5. 再次获取列表
print("\n[5] 获取蜡烛列表（新增后）")
test_get("/api/candles")

# 6. 获取统计
print("\n[6] 获取统计数据（新增后）")
test_get("/api/stats")

# 7. 按香型筛选
print("\n[7] 按香型筛选 '玫瑰'")
test_get("/api/candles?scent=玫瑰")

# 8. 点燃蜡烛
if candle1:
    print("\n[8] 点燃第一支蜡烛")
    test_put(f"/api/candles/{candle1['id']}/light")

# 9. 修改剩余比例
if candle1:
    print("\n[9] 修改第一支蜡烛剩余比例为 80%")
    test_put(f"/api/candles/{candle1['id']}/remaining", {"remaining_ratio": 80})

# 10. 删除第二支蜡烛
if candle2:
    print("\n[10] 删除第二支蜡烛")
    test_delete(f"/api/candles/{candle2['id']}")

# 11. 最终列表
print("\n[11] 最终蜡烛列表")
test_get("/api/candles")

print("\n" + "=" * 50)
print("API 测试完成！")
print("=" * 50)

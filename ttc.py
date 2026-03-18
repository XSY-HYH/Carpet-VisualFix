import math
from typing import Optional, Tuple, List

class TNT轨道计算器:
    def __init__(self):
        self.重力加速度 = 0.04
        self.空气阻力 = 0.98
        self.起始点: Optional[Tuple[float, float, float]] = None
        self.轨迹点: List[Tuple[float, float, float, float]] = []  # x, y, z, 已飞行时间
        
    def 开始(self, x: float, y: float, z: float) -> bool:
        try:
            self.起始点 = (x, y, z)
            self.轨迹点 = [(x, y, z, 0.0)]
            return True
        except:
            return False
    
    def TNT坐标(self, x: float, y: float, z: float) -> bool:
        try:
            if self.起始点 is None:
                return False
            last_x, last_y, last_z, last_t = self.轨迹点[-1]
            dx = x - last_x
            dy = y - last_y
            dz = z - last_z
            dt = math.sqrt(dx*dx + dy*dy + dz*dz) / math.sqrt(last_x*last_x + last_y*last_y + last_z*last_z + 1)
            new_t = last_t + max(0.05, dt)
            self.轨迹点.append((x, y, z, new_t))
            return True
        except:
            return False
    
    def 加速TNT坐标(self, vx: float, vy: float, vz: float, 时间: float) -> bool:
        try:
            if self.起始点 is None:
                return False
            last_x, last_y, last_z, last_t = self.轨迹点[-1]
            new_t = last_t + 时间
            # 考虑重力和阻力的轨迹计算
            t = 时间
            drag = self.空气阻力 ** t
            x = last_x + vx * t * drag
            y = last_y + vy * t - 0.5 * self.重力加速度 * t * t
            z = last_z + vz * t * drag
            self.轨迹点.append((x, y, z, new_t))
            return True
        except:
            return False
    
    def 距离(self, 索引1: int, 索引2: int) -> Optional[float]:
        try:
            if 索引1 < 0 or 索引1 >= len(self.轨迹点) or 索引2 < 0 or 索引2 >= len(self.轨迹点):
                return None
            x1, y1, z1, _ = self.轨迹点[索引1]
            x2, y2, z2, _ = self.轨迹点[索引2]
            return round(math.sqrt((x2-x1)**2 + (y2-y1)**2 + (z2-z1)**2), 1)
        except:
            return None
    
    def 结束(self) -> bool:
        try:
            self.起始点 = None
            self.轨迹点.clear()
            return True
        except:
            return False
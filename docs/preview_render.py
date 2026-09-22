"""Software-render jacket.obj with each tex/*.png to verify UVs, facing and pick icon->texture mapping."""
import glob, os, subprocess

RAW_DIR = '.preview/raw'
REN_DIR = '.preview/renders'
os.makedirs(RAW_DIR, exist_ok=True)
os.makedirs(REN_DIR, exist_ok=True)

# 1. export textures to raw RGBA with imagemagick
for src in sorted(glob.glob('tex/*.png')):
    base = os.path.splitext(os.path.basename(src))[0]
    dst = os.path.join(RAW_DIR, base + '.rgba')
    if not os.path.exists(dst):
        subprocess.run(['convert', src, '-depth', '8', 'rgba:' + dst], check=True)
print('raw exported')

# 2. parse obj
verts, uvs, norms, faces = [], [], [], []
cur = ''
with open('model/jacket.obj') as f:
    for line in f:
        if line.startswith('o '):
            cur = line.split()[1]
        elif line.startswith('v '):
            verts.append(tuple(map(float, line.split()[1:4])))
        elif line.startswith('vt '):
            uvs.append(tuple(map(float, line.split()[1:3])))
        elif line.startswith('vn '):
            norms.append(tuple(map(float, line.split()[1:4])))
        elif line.startswith('f '):
            idx = []
            for p in line.split()[1:]:
                a = p.split('/')
                idx.append((int(a[0]) - 1, int(a[1]) - 1, int(a[2]) - 1))
            faces.append((cur, idx))
print('faces:', len(faces))

# 3. mapping model-space (same math as planned for the mod; facing guess: front = +z)
parts = {}
for name in ['head', 'chest', 'armL', 'armR', 'legL', 'legR']:
    vs = [verts[i] for (o, idx) in faces for (i, _, _) in idx if o == name]
    xs = [v[0] for v in vs]; ys = [v[1] for v in vs]; zs = [v[2] for v in vs]
    parts[name] = dict(minx=min(xs), maxx=max(xs), miny=min(ys), maxy=max(ys),
                       minz=min(zs), maxz=max(zs))
feet = min(parts['legL']['miny'], parts['legR']['miny'])
headTop = parts['head']['maxy']
S = 32.0 / (headTop - feet)
print('scale S =', S)
CX = {'head': 0.0, 'chest': 0.0, 'armL': 6.0, 'armR': -6.0, 'legL': 1.95, 'legR': -1.95}
CZ = {'head': 0.0, 'chest': 0.0, 'armL': 0.0, 'armR': 0.0, 'legL': 0.78, 'legR': 0.78}
JT = {'head': (parts['head']['miny'], 0.0), 'chest': (parts['chest']['maxy'], 0.0),
      'armL': (parts['armL']['maxy'], 0.0), 'armR': (parts['armR']['maxy'], 0.0),
      'legL': (parts['legL']['maxy'], 12.0), 'legR': (parts['legR']['maxy'], 12.0)}

def xform(name, x, y, z):
    p = parts[name]; J, T = JT[name]
    cx = (p['minx'] + p['maxx']) / 2; cz = (p['minz'] + p['maxz']) / 2
    return (CX[name] + (x - cx) * S, T - (y - J) * S, CZ[name] + (z - cz) * S)

# sanity: transformed bboxes
for name in parts:
    p = parts[name]
    x0, y0, z0 = xform(name, p['minx'], p['miny'], p['minz'])
    x1, y1, z1 = xform(name, p['maxx'], p['maxy'], p['maxz'])
    print(f'{name}: x[{x0:.2f},{x1:.2f}] y[{min(y0,y1):.2f},{max(y0,y1):.2f}] z[{z0:.2f},{z1:.2f}]')

# precompute transformed tris: (x,y,z per vert, u,v, nx,ny,nz per vert)
tris = []
for (o, idx) in faces:
    tri = []
    for (vi, vti, vni) in idx:
        x, y, z = xform(o, *verts[vi])
        u, v = uvs[vti]
        nx, ny, nz = norms[vni]
        tri.append((x, y, z, u, v, nx, -ny, nz))  # normal: y flipped
    tris.append(tri)
print('tris:', len(tris))

SIZE = 260
LX, LY, LZ = 0.45, -0.75, 0.65
_ll = (LX * LX + LY * LY + LZ * LZ) ** 0.5
LX, LY, LZ = LX / _ll, LY / _ll, LZ / _ll

def render(px, back=False):
    img = bytearray(SIZE * SIZE * 3)
    zbuf = [-1e9] * (SIZE * SIZE)
    for tri in tris:
        if back:
            P = [(-p[0], p[1], -p[2], p[3], p[4], -p[5], p[6], -p[7]) for p in tri]
        else:
            P = tri
        xs = [p[0] for p in P]; ys = [p[1] for p in P]
        # project: x[-9,9]->[0,SIZE], y[-10.5,25.5]->[0,SIZE]
        sx = [(x + 9) / 18 * SIZE for x in xs]
        sy = [(y + 10.5) / 36 * SIZE for y in ys]
        x0 = max(0, int(min(sx))); x1 = min(SIZE - 1, int(max(sx)) + 1)
        y0 = max(0, int(min(sy))); y1 = min(SIZE - 1, int(max(sy)) + 1)
        if x1 < x0 or y1 < y0:
            continue
        (ax, ay), (bx, by), (cx, cy) = (sx[0], sy[0]), (sx[1], sy[1]), (sx[2], sy[2])
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9:
            continue
        z0, z1, z2 = P[0][2], P[1][2], P[2][2]
        u0, u1, u2 = P[0][3], P[1][3], P[2][3]
        v0, v1, v2 = P[0][4], P[1][4], P[2][4]
        nx = (P[0][5] + P[1][5] + P[2][5]) / 3
        ny = (P[0][6] + P[1][6] + P[2][6]) / 3
        nz = (P[0][7] + P[1][7] + P[2][7]) / 3
        nl = (nx * nx + ny * ny + nz * nz) ** 0.5 or 1.0
        shade = 0.35 + 0.65 * max(0.0, (nx * LX + ny * LY + nz * LZ) / nl)
        for yy in range(y0, y1 + 1):
            w1n = (by - cy) * 0.0  # placeholder (loop-invariant hoisting below)
            for xx in range(x0, x1 + 1):
                w1 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
                w2 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
                w0 = 1.0 - w1 - w2
                if w0 < 0 or w1 < 0 or w2 < 0:
                    continue
                z = w0 * z0 + w1 * z1 + w2 * z2
                k = yy * SIZE + xx
                if z <= zbuf[k]:
                    continue
                zbuf[k] = z
                u = (w0 * u0 + w1 * u1 + w2 * u2) % 1.0
                vv = (1.0 - (w0 * v0 + w1 * v1 + w2 * v2)) % 1.0
                tx = int(u * 1024) % 1024; ty = int(vv * 1024) % 1024
                ti = (ty * 1024 + tx) * 4
                o3 = k * 3
                img[o3] = min(255, int(px[ti] * shade))
                img[o3 + 1] = min(255, int(px[ti + 1] * shade))
                img[o3 + 2] = min(255, int(px[ti + 2] * shade))
    return bytes(img)

def save_ppm(path, img):
    with open(path, 'wb') as f:
        f.write(f'P6\n{SIZE} {SIZE}\n255\n'.encode() + img)

import time
for raw in sorted(glob.glob(os.path.join(RAW_DIR, '*.rgba'))):
    base = os.path.splitext(os.path.basename(raw))[0]
    with open(raw, 'rb') as f:
        px = f.read()
    t0 = time.time()
    save_ppm(os.path.join(REN_DIR, base + '.front.ppm'), render(px, False))
    print(base, 'front', f'{time.time()-t0:.1f}s')
    if base == 'act_stalker_hero':
        t0 = time.time()
        save_ppm(os.path.join(REN_DIR, base + '.back.ppm'), render(px, True))
        print(base, 'back', f'{time.time()-t0:.1f}s')
print('DONE')

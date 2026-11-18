#!/usr/bin/env python3
"""Generates placeholder 256x256 skins + all data/lang JSON.
Replace src/main/resources/assets/dragonlords/textures/entity/<id>_dragon.png with art based on your references.
UV layout is documented in DragonModel.java (texOffs values)."""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', 'dragonlords')
DATA = os.path.join(ROOT, 'data', 'dragonlords')

TYPES = {
 'fire':   dict(name='Fire Dragon',   base=(150,30,20),  dark=(70,12,10),   accent=(255,140,30),  membrane=(110,20,15), horn=(30,25,25),    eye=(255,220,60),  drop='minecraft:blaze_rod',
                biomes=['minecraft:desert','minecraft:badlands','minecraft:eroded_badlands','minecraft:wooded_badlands','minecraft:savanna_plateau']),
 'ice':    dict(name='Ice Dragon',    base=(170,210,235),dark=(90,130,170), accent=(235,250,255),membrane=(150,200,240),horn=(220,240,255), eye=(80,200,255),  drop='minecraft:packed_ice',
                biomes=['minecraft:snowy_plains','minecraft:ice_spikes','minecraft:snowy_taiga','minecraft:frozen_peaks','minecraft:snowy_slopes','minecraft:grove','minecraft:snowy_beach']),
 'forest': dict(name='Forest Dragon', base=(40,120,55),  dark=(18,60,28),   accent=(140,200,70), membrane=(60,140,60),  horn=(90,70,40),    eye=(230,255,60),  drop='minecraft:slime_ball',
                biomes=['minecraft:jungle','minecraft:sparse_jungle','minecraft:bamboo_jungle','minecraft:mangrove_swamp']),
 'ocean':  dict(name='Ocean Dragon',  base=(25,110,150), dark=(10,50,90),   accent=(80,220,200), membrane=(30,140,170), horn=(200,230,225), eye=(120,255,230), drop='minecraft:prismarine_shard',
                biomes=['#minecraft:is_ocean']),
 'storm':  dict(name='Storm Dragon',  base=(75,80,105),  dark=(35,38,58),   accent=(120,170,255),membrane=(60,65,95),   horn=(190,195,210), eye=(170,220,255), drop='minecraft:copper_ingot',
                biomes=['minecraft:windswept_hills','minecraft:windswept_forest','minecraft:windswept_gravelly_hills','minecraft:jagged_peaks','minecraft:stony_peaks']),
}

def mix(a, b, t): return tuple(int(a[i]*(1-t) + b[i]*t) for i in range(3))
def clamp(c): return tuple(max(0, min(255, v)) for v in c)

def fill(img, box, fn):
    x0, y0, w, h = box
    for y in range(y0, y0+h):
        for x in range(x0, x0+w):
            img.putpixel((x, y), clamp(fn(x - x0, y - y0, x, y)))

def make_texture(key, p):
    rnd = random.Random(key)
    img = Image.new('RGB', (256, 256))
    cellnoise = {}
    def scales(lx, ly, x, y):
        row = y // 4; col = (x + (row % 2) * 2) // 4
        n = cellnoise.setdefault((row, col), rnd.random())
        c = mix(p['dark'], p['base'], 0.35 + 0.65 * n)
        if x % 4 == 0 or y % 4 == 0: c = mix(c, p['dark'], 0.45)
        if (x % 4 == 2 and y % 4 == 2): c = mix(c, p['accent'], 0.18)
        j = rnd.randint(-5, 5)
        return (c[0]+j, c[1]+j, c[2]+j)
    fill(img, (0, 0, 256, 256), scales)

    # belly (body bottom face)
    fill(img, (42, 0, 14, 28), lambda lx, ly, x, y: mix(mix(p['accent'], p['base'], 0.5), p['dark'], 0.25 if ly % 3 == 0 else 0))
    # membranes with veins
    def membrane(lx, ly, x, y):
        c = mix(p['membrane'], p['accent'], 0.18 * (lx % 18) / 18)
        if (lx + ly) % 7 == 0 or (lx - ly) % 9 == 0: c = mix(c, p['dark'], 0.5)
        return c
    fill(img, (0, 96, 72, 19), membrane)
    fill(img, (80, 96, 84, 21), membrane)
    # wing bones, horns, spikes
    bone = lambda lx, ly, x, y: mix(p['horn'], p['dark'], 0.3 if (lx % 6 == 0) else 0)
    for box in [(0, 88, 42, 6), (80, 88, 48, 4), (190, 44, 16, 8), (120, 64, 10, 5)]:
        fill(img, box, bone)
    # eyes on both head side faces
    for ex in (83, 99):
        fill(img, (ex - 1, 53, 5, 4), lambda *a: p['dark'])
        fill(img, (ex, 54, 3, 2), lambda *a: p['eye'])
    # saddle
    def saddle(lx, ly, x, y):
        if lx % 5 == 0 and ly % 2 == 0 or ly in (0, 13): return (205, 170, 110)
        return (110 + rnd.randint(-4, 4), 70, 40)
    fill(img, (100, 120, 44, 14), saddle)
    return img

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2)

lang = {
    'key.categories.dragonlords': 'Dragon Lords',
    'key.dragonlords.breath': 'Dragon Breath (hold)',
    'key.dragonlords.charge': 'Dragon Charge',
}
for key, p in TYPES.items():
    ent = f'{key}_dragon'
    make_texture(key, p).save(os.path.join(ASSETS, 'textures', 'entity', f'{ent}.png'))
    lang[f'entity.dragonlords.{ent}'] = p['name']
    lang[f'item.dragonlords.{ent}_spawn_egg'] = p['name'] + ' Spawn Egg'
    write(os.path.join(ASSETS, 'models', 'item', f'{ent}_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    write(os.path.join(DATA, 'tags', 'worldgen', 'biome', f'{key}_dragon_habitat.json'), {'values': p['biomes']})
    write(os.path.join(DATA, 'neoforge', 'biome_modifier', f'{ent}.json'), {
        'type': 'neoforge:add_spawns',
        'biomes': f'#dragonlords:{key}_dragon_habitat',
        'spawners': {'type': f'dragonlords:{ent}', 'weight': 4, 'minCount': 1, 'maxCount': 1}})
    write(os.path.join(DATA, 'loot_table', 'entities', f'{ent}.json'), {
        'type': 'minecraft:entity',
        'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:bone',
                'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 3, 'max': 8}}]}]},
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': p['drop'],
                'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 6}}]}]}]})
write(os.path.join(ASSETS, 'lang', 'en_us.json'), lang)
print('generated')

"""Build 'AcctCalc Seven': an upright seven-segment font whose segments join without seams."""
import pathops
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen

UPM, W, H, T, X0, ADV = 1000, 500, 760, 78, 50, 600
S = {  # segment rectangles (x1, y1, x2, y2), y up
 'a': (X0, H - T, X0 + W, H), 'd': (X0, 0, X0 + W, T), 'g': (X0, H / 2 - T / 2, X0 + W, H / 2 + T / 2),
 'f': (X0, H / 2 - T / 2, X0 + T, H), 'b': (X0 + W - T, H / 2 - T / 2, X0 + W, H),
 'e': (X0, 0, X0 + T, H / 2 + T / 2), 'c': (X0 + W - T, 0, X0 + W, H / 2 + T / 2),
}
DIGITS = {'0': 'abcdef', '1': 'bc', '2': 'abged', '3': 'abgcd', '4': 'fgbc', '5': 'afgcd', '6': 'afgedc',
          '7': 'abc', '8': 'abcdefg', '9': 'abcdfg', 'minus': 'g', 'E': 'adefg', 'r': 'eg', 'o': 'cdeg'}

def rects_glyph(rects):
    p = pathops.Path()
    for x1, y1, x2, y2 in rects:
        p.moveTo(x1, y1); p.lineTo(x2, y1); p.lineTo(x2, y2); p.lineTo(x1, y2); p.close()
    p.simplify(fix_winding=True)          # union: one outline, no seams, no overlaps
    pen = TTGlyphPen(None)
    p.draw(pen)
    return pen.glyph()

glyphs, adv, cmap = {}, {}, {}
def add(name, rects, width, *codepoints):
    glyphs[name] = rects_glyph(rects) if rects else TTGlyphPen(None).glyph()
    adv[name] = width
    for c in codepoints: cmap[c] = name

add('.notdef', [(X0, 0, X0 + W, H)], ADV)
add('space', [], ADV, 0x20)
names = {'0': 'zero', '1': 'one', '2': 'two', '3': 'three', '4': 'four', '5': 'five', '6': 'six', '7': 'seven', '8': 'eight', '9': 'nine'}
for ch, segs in DIGITS.items():
    n = names.get(ch, ch)
    cps = [ord(ch)] if len(ch) == 1 else []
    if ch == 'minus': cps = [0x2D, 0x2212]
    add(n, [S[s] for s in segs], ADV, *cps)
dot = 180
add('period', [((dot - T) / 2, 0, (dot + T) / 2, T)], dot, 0x2E)
add('comma', [((dot - T) / 2, 0, (dot + T) / 2, T), ((dot - T) / 2 + T * .45, -T * .9, (dot + T) / 2, 0)], dot, 0x2C)
add('colon', [((dot - T) / 2, H * .22, (dot + T) / 2, H * .22 + T), ((dot - T) / 2, H * .64, (dot + T) / 2, H * .64 + T)], dot, 0x3A)
q = 150
add('quotesingle', [((q - T * .7) / 2, H - T * 2.2, (q + T * .7) / 2, H)], q, 0x27, 0x2019)

order = list(glyphs)
fb = FontBuilder(UPM, isTTF=True)
fb.setupGlyphOrder(order)
fb.setupCharacterMap(cmap)
fb.setupGlyf(glyphs)
metrics = {}
for n in order:
    g = glyphs[n]; g.recalcBounds(None) if hasattr(g, 'recalcBounds') else None
    metrics[n] = (adv[n], getattr(g, 'xMin', 0) or 0)
fb.setupHorizontalMetrics(metrics)
fb.setupHorizontalHeader(ascent=900, descent=-200)
fb.setupNameTable({'familyName': 'AcctCalc Seven', 'styleName': 'Regular', 'copyright': 'Copyright 2026 tthomastsai',
                   'version': 'Version 1.000', 'licenseDescription': 'SIL Open Font License 1.1'})
fb.setupOS2(sTypoAscender=900, sTypoDescender=-200, usWinAscent=900, usWinDescent=200, sxHeight=380, sCapHeight=H)
fb.setupPost(isFixedPitch=0)
fb.save(__import__('sys').argv[1] if len(__import__('sys').argv) > 1 else 'AcctCalcSeven-Regular.ttf')
print('glyphs:', len(order))

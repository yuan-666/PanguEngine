package engine.gui.graphics;

import com.google.common.base.Strings;
import engine.graphics.font.Font;
import engine.graphics.font.FontManager;
import engine.graphics.font.TextMesh;
import engine.gui.misc.Pos;
import engine.gui.text.WrapText;

import java.util.stream.Collectors;

public final class WrapTextRenderer implements NodeRenderer<WrapText> {

    private WrapText wrapText;

    private boolean dirty = true;

    private float lineHeight;
    private LineMesh[] meshes;

    public WrapTextRenderer(WrapText wrapText) {
        this.wrapText = wrapText;
        wrapText.text().addChangeListener((observable, oldValue, newValue) -> dirty = true);
        wrapText.font().addChangeListener((observable, oldValue, newValue) -> dirty = true);
        wrapText.textWidth().addChangeListener((observable, oldValue, newValue) -> dirty = true);
    }

    private void bakeTextMesh() {
        dirty = false;
        String text = wrapText.getText();
        if (Strings.isNullOrEmpty(text)) {
            meshes = null;
            return;
        }
        Font font = wrapText.getFont();
        float textWidth = wrapText.getTextWidth();
        FontManager fontManager = FontManager.instance();

        var lines = text.lines().flatMap(str -> fontManager.wrapText(str, textWidth, font).stream()).collect(Collectors.toList());
        lineHeight = fontManager.computeTextHeight(text, font, textWidth) / lines.size();
        meshes = new LineMesh[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            meshes[i] = new LineMesh(fontManager.bakeTextMesh(line, font), fontManager.computeTextWidth(line, font));
        }
    }

    @Override
    public void render(WrapText text, Graphics graphics) {
        if (dirty) bakeTextMesh();
        if (meshes == null) return;

        graphics.setColor(text.getColor());
        Pos alignment = text.getTextAlignment();
        float leading = (float) text.getLeading();
        float y = 0;
        for (LineMesh mesh : meshes) {
            var y1 = y + (lineHeight * leading - lineHeight) / 2;
            switch (alignment.getHPos()) {
                case RIGHT:
                    graphics.drawText(mesh.mesh, text.getWidth() - mesh.width, y1);
                    break;
                case CENTER:
                    graphics.drawText(mesh.mesh, (text.getWidth() - mesh.width) / 2, y1);
                    break;
                case LEFT:
                    graphics.drawText(mesh.mesh, 0, y1);
                    break;
            }
            y += lineHeight * leading;
        }
    }

    private static class LineMesh {
        private TextMesh mesh;
        private float width;

        public LineMesh(TextMesh mesh, float width) {
            this.mesh = mesh;
            this.width = width;
        }
    }
}

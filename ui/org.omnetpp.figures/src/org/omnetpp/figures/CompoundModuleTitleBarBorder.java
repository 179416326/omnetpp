package org.omnetpp.figures;

import org.eclipse.draw2d.AbstractLabeledBorder;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Text;

import org.omnetpp.figures.misc.IDirectEditSupport;

public class CompoundModuleTitleBarBorder extends AbstractLabeledBorder
                                          implements IDirectEditSupport {

	private static Color defaultColor = ColorConstants.menuBackgroundSelected;

	private Insets padding = new Insets(1, 3, 2, 2);
	private Color fillColor = defaultColor;

	private Insets imgInsets;
	private Image image;
	private Dimension imageSize;
	private boolean titleVisible = true;

    // cached values to support the cell edit locator for DirectEdit support
    private IFigure hostFigure;
    private Point labelLoc;
    private Font labelFont;

	public CompoundModuleTitleBarBorder() {
    }

	public CompoundModuleTitleBarBorder(String s) {
		setLabel(s);
	}

	/**
	 * Calculates and returns the Insets for this border.
	 *
	 * @param figure the figure on which Insets calculations are based
	 * @return the calculated Insets
	 */
	@Override
    protected Insets calculateInsets(IFigure figure) {
		int titleBarHeight = 0;
		// check the image height
		if (image != null)
			titleBarHeight = Math.max(titleBarHeight, image.getImageData().height + padding.getHeight());

		// check the text height
		titleBarHeight = Math.max(titleBarHeight, getTextExtents(figure).height + padding.getHeight());

		return new Insets(titleBarHeight, 0, 0, 0);
	}

	/**
	 * Returns the background Color of this TitleBarBorder.
	 * @return the background color
	 */
	protected Color getBackgroundColor() {
		return fillColor;
	}

	/**
	 * Returns this TitleBarBorder's padding. Padding provides spacing along the sides of the
	 * TitleBarBorder. The default value is no padding along all sides.
	 *
	 * @return the Insets representing the space along the sides of the TitleBarBorder
	 */
	protected Insets getPadding() {
		return padding;
	}

	/**
	 * Returns <code>true</code> thereby filling up all the contents within its boundaries,
	 * eliminating the need by the figure to clip the boundaries and do the same.
	 *
	 * @see Border#isOpaque()
	 */
	@Override
    public boolean isOpaque() {
		return true;
	}

	/**
	 * @see Border#paint(IFigure, Graphics, Insets)
	 */
	public void paint(IFigure figure, Graphics g, Insets insets) {
		int lx, ly, imgx, imgy;
		int titleBarHeight = 0;

		Rectangle tRect = new Rectangle(getPaintRectangle(figure, insets));
		lx = imgx = tRect.x;
		ly = imgy = tRect.y;
		// compute image position
		if (image != null) {
			imgx += padding.left;
			imgy += padding.top;
			lx = imgx + image.getImageData().width;
			titleBarHeight = Math.max(titleBarHeight, image.getImageData().height + padding.getHeight());
		}

		// compute label position
		titleBarHeight = Math.max(titleBarHeight, getTextExtents(figure).height + padding.getHeight());
		lx += padding.left;
		// position the text in the middle of the bar
		ly += padding.top + (titleBarHeight - getTextExtents(figure).height - padding.getHeight())/2;
		tRect.height = Math.min(tRect.height, titleBarHeight);

		g.clipRect(tRect);
		g.setFont(getFont(figure));
		g.setForegroundColor(getTextColor());
		if (fillColor != null) {
			g.setBackgroundColor(fillColor);
			g.fillRectangle(tRect);
		}
        if (titleVisible)
            g.drawString(getLabel(), lx, ly);
		if (getImage() != null)
			g.drawImage(getImage(), imgx, imgy);

        // store the label bounds (in absolute coordinates )so cell editors
        // will be able to place the editor over it
        hostFigure = figure;
        labelLoc = new Point(lx, ly);
        labelFont = g.getFont();
	}

	/**
	 * Sets the background color of the area within the boundaries of this border. This is
	 * required as this border takes responsibility for filling up the region, as
	 * TitleBarBorders are always opaque.
	 *
	 * @param color the background color
	 */
	public void setBackgroundColor(Color color) {
		fillColor = color;
	}

	/**
	 * Sets the padding space to be applied on all sides of the border. The default value is
	 * no padding on all sides.
	 *
	 * @param all the value of the padding on all sides
	 */
	public void setPadding(int all) {
		padding = new Insets(all);
		invalidate();
	}

	/**
	 * Sets the padding space of this TitleBarBorder to the passed value. The default value is
	 * no padding on all sides.
	 *
	 * @param pad the padding
	 */
	public void setPadding(Insets pad) {
		padding = pad;
		invalidate();
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image img) {
		image = img;
		if (img == null)
			return;

		imageSize = new Dimension(image);
		imgInsets = new Insets();
		imgInsets.left = imageSize.width;
	}

    public CellEditorLocator getDirectEditCellEditorLocator() {
        // this is a special cell edit locator for compound modules
        return new CellEditorLocator() {
            public void relocate(CellEditor celleditor) {
                Text text = (Text)celleditor.getControl();
                // set the font size
                FontData data = labelFont.getFontData()[0];
                Dimension fontSize = new Dimension(0, data.getHeight());
                hostFigure.translateToAbsolute(fontSize);
                data.setHeight(fontSize.height);
                text.setFont(new Font(null, data));

                // and the location and size of the editor
                org.eclipse.swt.graphics.Point pref = text.computeSize(-1, -1);
                Point editorLoc = labelLoc.getTranslated(-1, 0);
                hostFigure.translateToAbsolute(editorLoc);

                text.setBounds(editorLoc.x-1, editorLoc.y, pref.x, pref.y);
            }
        };
    }

    public String getDirectEditText() {
        return getLabel();
    }

    public void setDirectEditTextVisible(boolean visible) {
        titleVisible = visible;
        invalidate();
    }
}
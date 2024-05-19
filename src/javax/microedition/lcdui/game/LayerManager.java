/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.lcdui.game;

import java.util.ArrayList;

import java.awt.Shape;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;

//图层管理
public class LayerManager
{

	private int nlayers;
	private Layer component[] = new Layer[4];
	//protected ArrayList<Layer> layers;

	protected Image canvas;
	protected PlatformGraphics gc;
	protected Shape clip;

	//裁剪区坐标及宽度
	protected int x;
	protected int y;
	protected int width;
	protected int height;


	public LayerManager()
	{
		//layers = new ArrayList<Layer>();

		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;

		canvas = Image.createImage(width, height);
		gc = canvas.platformImage.getGraphics();
		
		setViewWindow(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public void append(Layer l) { 
	
		//layers.add(l);
		removeImpl(l);
		addImpl(l, nlayers);
	}

	public Layer getLayerAt(int index) { 
		//return layers.get(index);
		if ((index < 0) || (index >= nlayers)) {
			throw new IndexOutOfBoundsException();
		}
		return component[index];
	}

	public int getSize() { 
		//return layers.size();
		return nlayers;
	}

	public void insert(Layer l, int index) { 
		//layers.add(index, l);
		if ((index < 0) || (index > nlayers) || (exist(l) && (index >= nlayers))) {
			throw new IndexOutOfBoundsException();
		}
		removeImpl(l);
		addImpl(l, index);
	}

	public void paint(Graphics g, int xdest, int ydest)
	{
		/* for(int i=0; i<layers.size(); i++)
		{
			drawLayer(g, xdest, ydest, layers.get(i));
		} */
		// save the original clip
		int clipX = g.getClipX();
		int clipY = g.getClipY();
		int clipW = g.getClipWidth();
		int clipH = g.getClipHeight();

		// translate the LayerManager co-ordinates to Screen co-ordinates
		//平移坐标系，向右，向下
		g.translate(xdest - x, ydest - y);
		// set the clip to view window
		//clipRect()方法是Graphics类中的一个方法，用于设置一个矩形的裁剪区域。这意味着在该矩形内的图形和文本将被绘制，而不在矩形内的部分则被裁剪掉
		g.clipRect(x, y, width, height);// save the original clip
		
		for (int i = nlayers; --i >= 0; ) {
			Layer comp = component[i];
			if (comp.visible) {
				comp.paint(g);
				
			}
		}
		
		g.translate(-xdest + x, -ydest + y);
		g.setClip(clipX, clipY, clipW, clipH);
	}


	public void remove(Layer l) { 
		//layers.remove(l);
		removeImpl(l);
	}

	public void setViewWindow(int wx, int wy, int wwidth, int wheight)
	{
		x = wx;
		y = wy;
		width = wwidth;
		height = wheight;
	}
	
	private void addImpl(Layer layer, int index) {
		if (nlayers == component.length) {
			Layer newcomponents[] = new Layer[nlayers + 4];
			System.arraycopy(component, 0, newcomponents, 0, nlayers);
			System.arraycopy(component, index, newcomponents,
					index + 1, nlayers - index);
			component = newcomponents;
		} else {
			System.arraycopy(component, index, component,
					index + 1, nlayers - index);
		}

		component[index] = layer;
		nlayers++;
	}

	private void removeImpl(Layer l) {
		if (l == null) {
			throw new NullPointerException();
		}

		for (int i = nlayers; --i >= 0; ) {
			if (component[i] == l) {
				remove(i);
			}
		}
	}

	private boolean exist(Layer l) {
		if (l == null) {
			return false;
		}

		for (int i = nlayers; --i >= 0; ) {
			if (component[i] == l) {
				return true;
			}
		}
		return false;
	}

	private void remove(int index) {
		System.arraycopy(component, index + 1,
				component, index,
				nlayers - index - 1);
		component[--nlayers] = null;
	}

}

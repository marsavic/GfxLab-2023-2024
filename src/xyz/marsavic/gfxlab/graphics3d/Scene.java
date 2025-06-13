package xyz.marsavic.gfxlab.graphics3d;

import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.graphics3d.solids.Nothing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public interface Scene {
	Solid solid();
	
	Collection<Light> lights();
	
	default Color colorBackground() {
		return Color.BLACK;
	}
	
	
	class Base implements Scene {
		
		protected Solid solid = Nothing.INSTANCE;
		protected final List<Light> lights = new ArrayList<>();
		protected Color colorBackground = Color.BLACK;
		
		@Override
		public Solid solid() {
			return solid;
		}
		
		@Override
		public Collection<Light> lights() {
			return lights;
		}
		
		@Override
		public Color colorBackground() {
			return colorBackground;
		}
		
	}
	
	
}

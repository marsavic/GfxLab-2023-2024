package xyz.marsavic.gfxlab.graphics3d;


import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;

public interface Solid {
	
	/**
	 * Returns the first hit of the ray into the surface of the solid, occurring strictly after the given time.
	 * If the ray misses the Solid, the hit at infinity is returned.
	 * All hits along the same line should alternate between entering and exiting hits, meaning that the sign of the
	 * dot product between the normal at the hit and the line direction should alternate. This should also hold for
	 * the hit at infinity.
	 */
	Hit firstHit(Ray ray, double afterTime);
	
	
	default Hit firstHit(Ray ray) {
		return firstHit(ray, 0);
	}
	
	
	/** Is there any hit between afterTime and beforeTime. */
	default boolean hitBetween(Ray ray, double afterTime, double beforeTime) {
		double t = firstHit(ray, afterTime).t();
		return t < beforeTime;
	}
	
	
	default Solid transformed(Affine t) {
		return new Solid() {
			final Affine tInv = t.inverse();
			final Affine tInvTransposed = tInv.transposeWithoutTranslation();
			
			@Override
			public Hit firstHit(Ray ray, double afterTime) {
				Ray rayO = tInv.at(ray);
				Hit hitO = Solid.this.firstHit(rayO, afterTime);
				Vec3 n = tInvTransposed.at(hitO.n());
				return hitO.withN(n);
			}
		};
	}
	
	
	/** The solid made of all the points contained in at least k of the given solids. */
	static Solid atLeast(int k, Solid... solids) {
		return (ray, afterTime) -> {
			int n = solids.length;
			Hit[] hits = new Hit[n];
			int[] d = new int[n];
			int count = 0;
			
			for (int i = 0; i < n; i++) {
				Hit hit = solids[i].firstHit(ray, afterTime);
				hits[i] = hit;
				boolean inside = hit.n().dot(ray.d()) > 0;
				d[i] = inside ? -1 : 1;
				count += inside ? 1 : 0;
			}
			
			boolean isInside = count >= k;
			
			while (true) {
				double tHitMin = Double.POSITIVE_INFINITY;
				int iMin = -1;
				for (int i = 0; i < n; i++) {
					double t = hits[i].t();
					if (t < tHitMin) {
						tHitMin = t;
						iMin = i;
					}
				}
				
				if (tHitMin == Double.POSITIVE_INFINITY) {
					return Hit.AtInfinity.axisAligned(ray.d(), isInside);
				}
				
				count += d[iMin];
				d[iMin] = -d[iMin];
				boolean wasInside = isInside;
				isInside = count >= k;
				if (wasInside != isInside) {
					return hits[iMin];
				}
				hits[iMin] = solids[iMin].firstHit(ray, tHitMin);
			}
		};
	}
	
	default Solid withMaterial(F1<Material, Vector> mapMaterial) {
		return (ray, afterTime) -> {
			Hit hit = Solid.this.firstHit(ray, afterTime);
			return hit.withMaterial(mapMaterial.at(hit.uv()));
		};
	}
	
	static Solid union(Solid... solids) {
		return atLeast(1, solids);
	}
	

	static Solid intersection(Solid... solids) {
		return atLeast(solids.length, solids);
	}
	
	
	default Solid complement() {
		return (ray, afterTime) -> {
			Hit hit = Solid.this.firstHit(ray, afterTime);
			return hit.inverted();
		};
	}
	
	
	static Solid difference(Solid a, Solid b) {
		return intersection(a, b.complement());
	}
}

package xyz.marsavic.gfxlab.graphics3d.scenes;

import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Affine;
import xyz.marsavic.gfxlab.graphics3d.Light;
import xyz.marsavic.gfxlab.graphics3d.Scene;
import xyz.marsavic.gfxlab.graphics3d.Solid;
import xyz.marsavic.gfxlab.graphics3d.solids.Group;
import xyz.marsavic.gfxlab.graphics3d.solids.SDF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static xyz.marsavic.gfxlab.graphics3d.solids.SDF.*;

public class TestSDF extends Scene.Base {
	
	public TestSDF() {
		SDF s = smoothUnion(
					ball(Vec3.xyz(-0.4, 0, 0), 0.3),
					box(Vec3.EXYZ.mul(0.4)),
					0.2
				);
				
		
		Collection<Solid> solids = new ArrayList<>();
		Collections.addAll(solids,
			s.transformed(Affine.IDENTITY
					.then(Affine.scaling(1.8))
					.then(Affine.rotationAboutX(-0.07))
					.then(Affine.rotationAboutY(-0.1))
			)
		);
		
		solid = Group.of(solids);
		
		Collections.addAll(lights,
			Light.p(Vec3.xyz( .5,  .5, -1.0).mul(2)),
			Light.p(Vec3.xyz(-.5,  .5, -1.0).mul(2))
		);
	}
}

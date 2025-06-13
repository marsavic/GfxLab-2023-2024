package xyz.marsavic.gfxlab.gui;

import com.google.common.collect.ImmutableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import xyz.marsavic.functions.A0;
import xyz.marsavic.functions.A1;
import xyz.marsavic.functions.A2;
import xyz.marsavic.functions.F0;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Matrix;
import xyz.marsavic.gfxlab.MatrixColor;
import xyz.marsavic.gfxlab.MatrixInts;
import xyz.marsavic.random.RNG;
import xyz.marsavic.resources.ResourceManagerMap;
import xyz.marsavic.time.Profiler;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;


public class UtilsGL {
	
	private static final Set<Profiler> profilers = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	
	
	public static Profiler profiler(Object object, String description) {
		String name =
				String.format("%08x", System.identityHashCode(object)) +
						" " +
						object.getClass().getSimpleName() +
						" " +
						description;
		
		Profiler profiler = new Profiler(name);
		profilers.add(profiler);
		return profiler;
	}
	
	
	/**
	 * Live profilers, but not a live collection.
	 * The returned collection is immutable and contains only the profilers present at the moment of calling.
	 */
	public static Collection<Profiler> profilers() {
		synchronized (profilers) {
			return ImmutableList.copyOf(profilers);
		}
	}
	
	
	// JavaFX helpers
	
	static final PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
	
	
	public static Image writeMatrixToImage(MatrixInts rawImage) {
		int sx = rawImage.width();
		int sy = rawImage.height();
		WritableImage image = new WritableImage(sx, sy);
		writeMatrixToImage(image, rawImage);
		return image;
	}
	

	
	
	public static void writeMatrixToImage(WritableImage outImage, Matrix<Integer> inMatrix) {
		int sx = inMatrix.size().xInt();
		int sy = inMatrix.size().yInt();
		
		if (inMatrix instanceof MatrixInts m) {
			outImage.getPixelWriter().setPixels(0, 0, sx, sy, pixelFormat, m.array(), 0, sx);
		} else {
			matricesInt.borrow(inMatrix.size(), true, m -> {
				m.copyFrom(inMatrix);
				outImage.getPixelWriter().setPixels(0, 0, sx, sy, pixelFormat, ((MatrixInts) m).array(), 0, sx);
			});
		}
	}
	
	
	public static Vector imageSize(Image image) {
		return Vector.xy(image.getWidth(), image.getHeight());
	}
	
	
	public static WritableImage createWritableImage(Vector size) {
		return new WritableImage(size.xInt(), size.yInt());
	}
	
	
	public static void saveImageToFileWithDialog(Image image) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Image");
		fileChooser.setInitialDirectory(new File("."));
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Image Files", "*.png"),
				new FileChooser.ExtensionFilter("All Files", "*.*")
		);
		File file = fileChooser.showSaveDialog(null);
		
		if (file != null) {
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static void copyImageToClipboard(Image image) {
		ClipboardContent content = new ClipboardContent();
		content.putImage(image);
		Clipboard.getSystemClipboard().setContent(content);
	}
	
	
	// --- Parallel utils -------
	
	
	private static final ExecutorService pool;
	
	public static <V> Future<V> submitTask(F0<V> f) {
		return pool.submit(() -> {
			try {
				return f.at();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		});
	}
	
	public static Future<?> submitTask(A0 a) {
		return pool.submit(() -> {
			try {
				a.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void submitTaskAndWait(A0 a) {
		Future<?> future = submitTask(a);
		while (true) { // TODO What to do on exception?
			try {
				future.get();
				break;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static <T> T futureGet(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static final int parallelism;
	
	
	static {
//		int p = ForkJoinPool.getCommonPoolParallelism() - 1;
		int p = (ForkJoinPool.getCommonPoolParallelism() + 1) / 2;
//		int p = 2;
		
		// 5 0.485
		// 6 0.380

/*
		try {
			boolean obsRunning = false;
			obsRunning |= ProcessHandle.allProcesses().anyMatch(ph -> ph.info().command().orElse("").contains("obs64")); // Windows
			obsRunning |= !Utils.runCommand("top -b -n 1 | grep \" obs\"").isEmpty(); // Linux
			obsRunning |= true;
			if (obsRunning) {
				p -= 3;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
*/
		parallelism = p;
		pool = new ForkJoinPool(parallelism);
//		pool = Executors.newVirtualThreadPerTaskExecutor();
	}
	
	
	public static void parallel(int iFrom, int iTo, IntConsumer action) {
		submitTaskAndWait(() -> IntStream.range(iFrom, iTo).parallel().forEach(action));
	}
	
	public static void parallel(int iTo, IntConsumer action) {
		parallel(0, iTo, action);
	}
	
	public static void parallelY(Vector size, IntConsumer action) {
		parallel(size.yInt(), action);
	}
	
	public static void parallelY(Vector size, A2<Integer, Integer> action) {
		parallelY(size, y -> {
			for (int x = 0; x < size.xInt(); x++) {
				action.execute(x, y);
			}
		});
	}
	
	public static void parallel(Vector size, A1<Vector> action) {
		parallelY(size, y -> {
			for (int x = 0; x < size.xInt(); x++) {
				action.execute(Vector.xy(x, y));
			}
		});
	}
	
	
	public static RNG[] rngArray(int n, long seed) {
		RNG seeds = new RNG(seed);
		RNG[] rngs = new RNG[n];
		for (int i = 0; i < n; i++) {
			rngs[i] = new RNG(seeds.nextLong());
		}
		return rngs;
	}
	
	
	// TODO remove - nondeterministic behavior.
	public static RNG[] rngArray(int n) {
		return rngArray(n, new RNG().nextLong());
	}
	
	
	public static final ResourceManagerMap<Vector, Matrix<Color>> matricesColor = new ResourceManagerMap<>(
			MatrixColor::new, (m, sz) -> ((MatrixColor) m).fillBlack()
	);

	public static final ResourceManagerMap<Vector, Matrix<Integer>> matricesInt = new ResourceManagerMap<>(
			MatrixInts::new, (m, sz) -> m.fill(0)
	);
	
}

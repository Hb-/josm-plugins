package org.openstreetmap.josm.plugins.mapillary.downloads;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import javax.imageio.ImageIO;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffDirectoryConstants;
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants;
import org.apache.sanselan.formats.tiff.fieldtypes.FieldType;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;

/**
 * Writes the images from the queue in the HD.
 * 
 * @author nokutu
 * @see MapillaryExportManager
 */
public class MapillaryExportWriterThread implements Runnable {

	private final String path;
	private final ArrayBlockingQueue<BufferedImage> queue;
	private final ArrayBlockingQueue<MapillaryImage> queueImages;
	private final int amount;
	private final ProgressMonitor monitor;

	public MapillaryExportWriterThread(String path,
			ArrayBlockingQueue<BufferedImage> queue,
			ArrayBlockingQueue<MapillaryImage> queueImages, int amount,
			ProgressMonitor monitor) {
		this.path = path;
		this.queue = queue;
		this.queueImages = queueImages;
		this.amount = amount;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		monitor.setCustomText("Downloaded 0/" + amount);
		File tempFile = null;
		BufferedImage img;
		MapillaryImage mimg = null;
		String finalPath = "";
		for (int i = 0; i < amount; i++) {
			try {
				img = queue.take();
				mimg = queueImages.take();
				finalPath = path + "/" + mimg.getKey();
				// Creates a temporal file that is going to be deleted after
				// writing the EXIF tags.
				tempFile = new File(finalPath + ".tmp");
				ImageIO.write(img, "jpg", tempFile);

				// Write EXIF tags
				TiffOutputSet outputSet = new TiffOutputSet();
				TiffOutputDirectory exifDirectory = outputSet
						.getOrCreateGPSDirectory();
				FieldType fieldType = TiffFieldTypeConstants.FIELD_TYPE_RATIONAL;
				TiffOutputField directionref = TiffOutputField.create(
						GPSTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF,
						outputSet.byteOrder, "T");
				exifDirectory.add(directionref);
				TiffOutputField direction = TiffOutputField.create(new TagInfo(
						"GPS Img Direction", 17, fieldType, 1,
						TiffDirectoryConstants.EXIF_DIRECTORY_GPS),
						outputSet.byteOrder, mimg.getCa());
				exifDirectory.add(direction);
				try {
					outputSet.setGPSInDegrees(mimg.getLatLon().lon(), mimg
							.getLatLon().lat());
				} catch (ImageWriteException e) {
					Main.error(e);
				}
				OutputStream os = new BufferedOutputStream(
						new FileOutputStream(finalPath + ".jpg"));
				new ExifRewriter().updateExifMetadataLossless(tempFile, os,
						outputSet);
				tempFile.delete();
				os.close();
			} catch (InterruptedException e) {
				Main.error(e);
			} catch (IOException e) {
				Main.error(e);
			} catch (ImageReadException e) {
				Main.error(e);
			} catch (ImageWriteException e) {
				Main.error(e);
			}

			// Increases the progress bar.
			monitor.worked(PleaseWaitProgressMonitor.PROGRESS_BAR_MAX / amount);
			monitor.setCustomText("Downloaded " + (i + 1) + "/" + amount);
		}
	}

}
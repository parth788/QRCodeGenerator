import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class EncodeQRExcel {
	public static void main(String[] args) throws Exception {

		generateQRCodeFromExcel(new File("StudentList.xlsx"));

	}

	private static void generateQRCodeFromExcel(File excelFile) throws Exception {
		int totalRecords = 0, QRCodeGenrerated = 0, QRCodeNotGenerated = 0;
		// Specify the URL or text to generate QR Code
		//String URL = "https://charusat.ac.in/getResultByID.php?id=";
		//String URL = "https://charusat.ac.in/";
		String URL;
		// Open the logo
		File logo = new File("charusat50x50.png");
		// Creating a Workbook from an Excel file (.xls or .xlsx)
		Workbook workbook = WorkbookFactory.create(excelFile);

		// Retrieving the number of sheets in the Workbook
		System.out.println("Workbook has data of " + workbook.getNumberOfSheets() + " Institutes ");

		Iterator<Sheet> sheetIterator = workbook.sheetIterator();
		System.out.println("Retrieving Students' ID from each Institute");
		while (sheetIterator.hasNext()) {
			Sheet sheet = sheetIterator.next();
			System.out.println("Generating QR Code for => " + sheet.getSheetName());
			String destDir = sheet.getSheetName();
			File destDirFile = new File("d:/" + destDir);
			if (destDirFile.exists()) {
				destDirFile.delete();
				System.out.println("Deleting existing directory => " + destDirFile);
			}
			destDirFile.mkdir();
			System.out.println("Created fresh directory => " + destDirFile);

			// Create a DataFormatter to format and get each cell's value as String
			DataFormatter dataFormatter = new DataFormatter();

			Iterator<Row> rowIterator = sheet.rowIterator();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// Now let's iterate over the columns of the current row
				Iterator<Cell> cellIterator = row.cellIterator();

				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					String cellValue = dataFormatter.formatCellValue(cell);
					String ID = cellValue;
					// Uncomment following for the color QR code generator with Logo
					if(!ID.contains("DR")) {
						URL = "https://charusat.ac.in/getResultByID.php?id=";
					}else {
						URL = "https://charusat.ac.in/getPHDResult.php?id=";
					}
					File destFile = encodeQRCode(URL, destDir, ID, logo, 300);
					// Following is for Black and White QR Code without Logo
					// File destFile = createQRImage(new File("d:/" + destDir + "/" + ID + ".png"),
					// URL+ID, 300, "png");

					System.out.println("QR Code for " + URL + ID + " Generated!!!");
					totalRecords++;
					String decodedText = decodeQRCode(destFile);

					if (decodedText == null) {
						QRCodeNotGenerated++;
						System.out.println("No QR Code found in the image for " + destFile);
						// if (destFile.delete()) {
						// System.out.println("File Deleted "+ destFile);
						// }
					} else {
						QRCodeGenrerated++;
						System.out.println("Decoded text = " + decodedText);
					}
					// System.out.println("'"+cellValue +"'");
				}
			}
		}
		System.out.println("Total Records: " + totalRecords);
		System.out.println("Total QRCodeGenerated: " + QRCodeGenrerated);
		System.out.println("Total QRCodeNotGenerated: " + QRCodeNotGenerated);
		workbook.close();
	}

	private static File encodeQRCode(String url, String destDir, String ID, File logo, int size) {
		File fileDest = new File("d:/" + destDir + "/" + ID + ".png");
		if (fileDest.length() > 0) {
			// System.out.println("File exists "+ fileDest);
			return fileDest;
		}
		Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		hints.put(EncodeHintType.MARGIN, 0); /* default = 4 */

		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix bitMatrix = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			// Create a qr code with the url as content and a size of 300x300 px
			bitMatrix = writer.encode(url + ID, BarcodeFormat.QR_CODE, size, size, hints);

			MatrixToImageConfig config = new MatrixToImageConfig(0xFF054E9A, MatrixToImageConfig.WHITE);
			

			// Load QR image
			BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

			BufferedImage logoImage = ImageIO.read(logo);

			// Calculate the delta height and width between QR code and logo
			int deltaHeight = qrImage.getHeight() - logoImage.getHeight();
			int deltaWidth = qrImage.getWidth() - logoImage.getWidth();

			// Initialize combined image
			BufferedImage combined = new BufferedImage(qrImage.getHeight(), qrImage.getWidth(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) combined.getGraphics();

			// Write QR code to new image at position 0/0
			g.drawImage(qrImage, 0, 0, null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

			// Write logo into combine image at position (deltaWidth / 2) and
			// (deltaHeight / 2). Background: Left/Right and Top/Bottom must be
			// the same space for the logo to be centered
			g.drawImage(logoImage, (int) Math.round(deltaWidth / 2), (int) Math.round(deltaHeight / 2), null);

			// Write combined image as PNG to OutputStream

			ImageIO.write(combined, "png", fileDest);

		} catch (WriterException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
		return fileDest;
	}

	private static String decodeQRCode(File qrCodeimage) throws IOException {
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
		hints.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);

		BufferedImage bufferedImage = ImageIO.read(qrCodeimage);
		LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		try {
			Result result = new MultiFormatReader().decode(bitmap, hints);
			return result.getText();
		} catch (NotFoundException e) {
			System.out.println("There is no QR code in the image " + qrCodeimage);
			return null;
		}
	}

	private static File createQRImage(File qrFile, String qrCodeText, int size, String fileType)
			throws WriterException, IOException {
		// Create the ByteMatrix for the QR-Code that encodes the given String
		Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix byteMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
		// Make the BufferedImage that are to hold the QRCode
		int matrixWidth = byteMatrix.getWidth();
		BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
		image.createGraphics();

		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setColor(Color.WHITE);
		
		graphics.fillRect(0, 0, matrixWidth, matrixWidth);
		// Paint and save the image using the ByteMatrix
		graphics.setColor(Color.BLACK);

		for (int i = 0; i < matrixWidth; i++) {
			for (int j = 0; j < matrixWidth; j++) {
				if (byteMatrix.get(i, j)) {
					graphics.fillRect(i, j, 1, 1);
				}
			}
		}
		ImageIO.write(image, fileType, qrFile);
		return qrFile;
	}
}

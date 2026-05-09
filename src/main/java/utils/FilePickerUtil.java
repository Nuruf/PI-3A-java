package utils;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;

public class FilePickerUtil {

    /**
     * Opens a FileChooser restricted to PDF files.
     * @param ownerWindow The window where the dialog will appear.
     * @param title The title of the dialog.
     * @return The selected File, or null if canceled.
     */
    public static File pickPdf(Window ownerWindow, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        // Default to user home directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // Restriction to PDF
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Document PDF", "*.pdf")
        );

        return fileChooser.showOpenDialog(ownerWindow);
    }

    /**
     * Checks if a file is within the size limit.
     * @param file The file to check.
     * @param maxMb Maximum size allowed in Megabytes.
     * @return true if valid, false otherwise.
     */
    public static boolean isSizeValid(File file, int maxMb) {
        if (file == null) return false;
        long limit = (long) maxMb * 1024 * 1024;
        return file.length() <= limit;
    }
}
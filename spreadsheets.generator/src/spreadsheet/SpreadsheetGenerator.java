package spreadsheet;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import password.PasswordGenerator;
import password.RandomPasswordGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used for generation new spreadsheets with login-password pairs using given spreadsheets. It also generates .csv
 * ejudge-readable file with users data.
 */
public class SpreadsheetGenerator {
    private String loginTemplate = "login-%03d";
    private String nameTemplate = "";

    private PasswordGenerator generator = new RandomPasswordGenerator();
    private int passwordLength = 9;

    private List<Integer> neededColumns = Collections.emptyList();

    /**
     * Sets login template for using in {@link String#format(String, Object...)} method.
     *
     * @param loginTemplate given template.
     */
    public void setLoginTemplate(final String loginTemplate) {
        this.loginTemplate = loginTemplate;
    }

    /**
     * Sets name template and needed columns' numbers for using in {@link String#format(String, Object...)} method.
     *
     * @param neededColumns list of columns' numbers that used to generate names for ejudge.
     * @param nameTemplate given template.
     */
    public void setNameTemplate(final List<Integer> neededColumns, final String nameTemplate) {
        this.neededColumns = neededColumns;
        this.nameTemplate = nameTemplate;
    }

    /**
     * Sets password length.
     *
     * @param passwordLength given password length.
     */
    public void setPasswordLength(final int passwordLength) {
        this.passwordLength = passwordLength;
    }

    /**
     * Sets {@link PasswordGenerator}.
     *
     * @param generator given generator.
     */
    public void setPasswordGenerator(final PasswordGenerator generator) {
        this.generator = generator;
    }

    private String generateLogin(final int id) {
        return String.format(loginTemplate, id);
    }

    private String generatePassword() {
        return generator.nextPassword(passwordLength);
    }

    private String generateName(final List<String> data) {
        return String.format(nameTemplate, data.toArray());
    }

    private boolean isBlank(final Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != XSSFCell.CELL_TYPE_BLANK) {
                return false;
            }
        }
        return true;
    }

    private void copyRow(final Row from, final Row to) {
        for (int i = 0; i < from.getLastCellNum(); i++) {
            to.createCell(i);
            Cell fromCell = from.getCell(i);

            if (fromCell == null) {
                continue;
            }

            switch (fromCell.getCellType()) {
                case XSSFCell.CELL_TYPE_BLANK:
                    to.getCell(i).setCellValue("");
                    break;
                case XSSFCell.CELL_TYPE_BOOLEAN:
                    to.getCell(i).setCellValue(fromCell.getBooleanCellValue());
                    break;
                case XSSFCell.CELL_TYPE_NUMERIC:
                    to.getCell(i).setCellValue(fromCell.getNumericCellValue());
                    break;
                case XSSFCell.CELL_TYPE_STRING:
                    to.getCell(i).setCellValue(fromCell.getStringCellValue());
                    break;
                default:
                    to.getCell(i).setCellValue("???");
            }
        }
    }

    private void addCellToRow(final Row row, final String value) {
        row.createCell(row.getLastCellNum());
        row.getCell(row.getLastCellNum() - 1).setCellValue(value);
    }

    private List<String> getNeeddedCells(final Row row) {
        List<String> result = new ArrayList<>(neededColumns.size());

        for (Integer col : neededColumns) {
            Cell cell = row.getCell(col);

            if (cell == null) {
                result.add("");
                continue;
            }

            switch (cell.getCellType()) {
                case XSSFCell.CELL_TYPE_BLANK:
                    result.add("");
                    break;
                case XSSFCell.CELL_TYPE_BOOLEAN:
                    result.add(String.valueOf(cell.getBooleanCellValue()));
                    break;
                case XSSFCell.CELL_TYPE_NUMERIC:
                    result.add(String.valueOf((int)cell.getNumericCellValue()));
                    break;
                case XSSFCell.CELL_TYPE_STRING:
                    result.add(cell.getStringCellValue());
                    break;
                default:
                    result.add("???");
            }
        }

        return result;
    }

    /**
     * Generates .xlsx file with given name that consists of information stored in given .xlsx file and generated
     * login-password pairs and .csv file in ejudge-readable format that stores users data.
     *
     * @param inputFile path to given .xslx file.
     * @param outputFile path to generated .xslx file.
     * @param ejudgeOutputFile path to ejudge-readable .csv file or <code>null</code> if it's not needed.
     * @param hasCaption is <code>true</code> if the first line in given file is a caption.
     * @param sheetName needed sheet name in given file.
     *
     * @throws IOException if I/O error happened while reading or writing files.
     */
    public void generate(final String inputFile, final String outputFile, final String ejudgeOutputFile, final boolean hasCaption, final String sheetName) throws IOException {
        if (inputFile == null || outputFile == null || sheetName == null) {
            throw new IllegalArgumentException("Some of given arguments are null");
        }

        InputStream inputStream = new FileInputStream(inputFile);
        OutputStream outputStream = new FileOutputStream(outputFile);
        CSVWriter ejudgeWriter = null;

        if (ejudgeOutputFile != null) {
            ejudgeWriter = new CSVWriter(new FileWriter(ejudgeOutputFile), ';', '\"', '\n', System.lineSeparator());
            ejudgeWriter.writeNext(new String[]{"login", "password", "cnts_password", "name"}, false);
        }

        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet sheet = workbook.getSheet(sheetName);

        Workbook outputWorkbook = new XSSFWorkbook();
        Sheet outputSheet = outputWorkbook.createSheet(sheetName);

        int start = 0;
        if (hasCaption) {
            start++;

            Row caption = sheet.getRow(0);
            Row outputCaption = outputSheet.createRow(0);

            copyRow(caption, outputCaption);
            addCellToRow(outputCaption, "Логин");
            addCellToRow(outputCaption, "Пароль");
        }

        for (int i = start; i < sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            if (isBlank(row)) {
                continue;
            }

            Row outputRow = outputSheet.createRow(i);

            String login = generateLogin(i + (hasCaption ? 0 : 1));
            String password = generatePassword();
            String name = generateName(getNeeddedCells(row));

            copyRow(row, outputRow);
            addCellToRow(outputRow, login);
            addCellToRow(outputRow, password);

            if (ejudgeWriter != null) {
                ejudgeWriter.writeNext(new String[]{login, password, password, name}, false);
            }
        }

        outputWorkbook.write(outputStream);

        workbook.close();
        outputWorkbook.close();

        inputStream.close();
        outputStream.close();

        if (ejudgeWriter != null) {
            ejudgeWriter.close();
        }
    }
}

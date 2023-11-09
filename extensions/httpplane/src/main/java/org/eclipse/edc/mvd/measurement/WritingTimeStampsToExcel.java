/*
*  Copyright (c) 2022 Jens Feser
*
*/

package org.eclipse.edc.mvd.measurement;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class WritingTimeStampsToExcel {
    public void writeToExcel(Instant sendTimeStamp, Instant receivedTimeStamp) throws IOException {
        File test = new File("/app/resources/result.xlsx");
        try (FileInputStream fis = new FileInputStream(test)) {
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(4); // Goes to first Workbook

                int lastRowIndex = sheet.getLastRowNum();

                Row row;

                if (sheet.getRow(lastRowIndex) == null || sheet.getRow(lastRowIndex).getCell(0) == null ||
                        sheet.getRow(lastRowIndex).getCell(0).getStringCellValue().isEmpty()) {

                    row = sheet.createRow(lastRowIndex);
                } else {

                    row = sheet.createRow(lastRowIndex + 1);
                }



                // insert sendTimeStamp to cell
                Cell cellSend = row.createCell(0);
                cellSend.setCellValue(sendTimeStamp.toString());

                // insert receivedTimeStamp to cell
                Cell cellReceived = row.createCell(1);
                cellReceived.setCellValue(receivedTimeStamp.toString());

                // Calculate Difference
                Duration duration = Duration.between(sendTimeStamp, receivedTimeStamp);

                long hours = duration.toHours();
                int minutes = duration.toMinutesPart();
                int seconds = duration.toSecondsPart();
                int millis = duration.toMillisPart();
                int nanos = duration.toNanosPart();

                // Format the string
                String strDuration = String.format("%02d:%02d:%02d.%03d%06d",
                        hours, minutes, seconds, millis, nanos);

                // Insert Difference to cell
                Cell cellDiff = row.createCell(2);
                cellDiff.setCellValue(strDuration);

                //write changes to file and close it
                try (FileOutputStream outputStream = new FileOutputStream("/app/resources/result.xlsx")) {
                    workbook.write(outputStream);
                }
            }
        }
        System.out.println("Writing Data to Excel successful");
    }
}

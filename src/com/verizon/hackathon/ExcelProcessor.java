Enter file contents herepackage com.verizon.hackathon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelProcessor {
	
	public static ArrayList<Query> readQueries(){
		
		ArrayList<Query> queries = new ArrayList<Query>();
		
		try
        {
            FileInputStream file = new FileInputStream(new File("H:/AD/MyProject/Hackathon/Hackathon-SQL.xlsx"));
 
            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);
 
            //Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);
 
            //Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Ignore first row which is a header
            rowIterator.next();	
            
            while (rowIterator.hasNext()) 
            {
                Row row = rowIterator.next();
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                
                Query query = new Query();
                Cell cell = cellIterator.next();
                query.setQueryId(String.valueOf(cell.getNumericCellValue()));
                cell = cellIterator.next();
                query.setQueryText(cell.getStringCellValue());
                
                queries.add(query);
            }
            file.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
		return queries;
	}
	
	public static void writeQueries(ArrayList<QueryMetaData> queryMetaDatas){
		
		//Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook(); 
         
        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet("MetaData");
          
        //Iterate over data and write to sheet
        Iterator<QueryMetaData> metadataIter = queryMetaDatas.iterator();
        int rownum = 0;

        //Set Header
        Row row = sheet.createRow(rownum++);
    	Cell cell = row.createCell(0);
    	cell.setCellValue("Type of SQL Statement");
    	cell = row.createCell(1);
    	cell.setCellValue("SQL Identifier");
    	cell = row.createCell(2);
    	cell.setCellValue("Target Schema ? Database");
    	cell = row.createCell(3);
    	cell.setCellValue("Target Table");
    	cell = row.createCell(4);
    	cell.setCellValue("Target Column");
    	cell = row.createCell(5);
    	cell.setCellValue("Source Schema / Database");
    	cell = row.createCell(6);
    	cell.setCellValue("Source Table");
    	cell = row.createCell(7);
    	cell.setCellValue("Source Column");
    	cell = row.createCell(8);
    	cell.setCellValue("Transformation");
    	
    	while(metadataIter.hasNext()){
        	QueryMetaData qmd = metadataIter.next();
        	
        	row = sheet.createRow(rownum++);
        	cell = row.createCell(0);
        	cell.setCellValue(qmd.getQueryType());
        	cell = row.createCell(1);
        	cell.setCellValue(qmd.getQueryId());
        	cell = row.createCell(2);
        	cell.setCellValue(qmd.getTargetSchema());
        	cell = row.createCell(3);
        	cell.setCellValue(qmd.getTargetTable());
        	cell = row.createCell(4);
        	cell.setCellValue(qmd.getTargetColumn());
        	cell = row.createCell(5);
        	cell.setCellValue(qmd.getSourceSchema());
        	cell = row.createCell(6);
        	cell.setCellValue(qmd.getSourceTable());
        	cell = row.createCell(7);
        	cell.setCellValue(qmd.getSourceColumn());
        	cell = row.createCell(8);
        	cell.setCellValue(qmd.getTransformation());
        }
       
        try
        {
            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(new File("H:/AD/MyProject/Hackathon/SQL_MetaData.xlsx"));
            workbook.write(out);
            out.close();
            System.out.println("SQL_MetaData.xlsx written successfully on disk.");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
	}
}

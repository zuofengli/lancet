/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uwm.jiaoduan.lab;

import org.apache.pdfbox.exceptions.InvalidPasswordException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.pdfbox.util.TextPosition;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import java.util.List;

/**
 * This is an example on how to extract text from a specific area on the PDF document.
 *
 * Usage: java org.apache.pdfbox.examples.util.ExtractTextByArea &lt;input-pdf&gt;
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.2 $
 */
public class ExtractTextByArea
{
    private ExtractTextByArea()
    {
        //utility class and should not be constructed.
    }


    /**
     * This will print the documents text in a certain area.
     *
     * @param args The command line arguments.
     *
     * @throws Exception If there is an error parsing the document.
     */
    public static void main( String[] args ) throws Exception
    {
    	args = new String[]{"test.pdf"};
        if( args.length != 1 )
        {
            usage();
        }
        else
        {
            PDDocument document = null;
            try
            {
                document = PDDocument.load( args[0] );
                if( document.isEncrypted() )
                {
                    try
                    {
                        document.decrypt( "" );
                    }
                    catch( InvalidPasswordException e )
                    {
                        System.err.println( "Error: Document is encrypted with a password." );
                        System.exit( 1 );
                    }
                }
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition( true );
                //Rectangle rect = new Rectangle( 99,219,80,15 );
                //convert xfdf coordinate to rectangle
                
                Rectangle2D.Double rect = new Rectangle2D.Double();

                List allPages = document.getDocumentCatalog().getAllPages();
                PDPage firstPage = (PDPage)allPages.get( 0 );
                
                double pageHeight = firstPage.getMediaBox().getHeight();
                
                //125.824906,672.39502,390.577109,694.679017
                double[] coords = new double[] {58.50615,500.847504,302.919073,552.419312};
                //rect.height = 694.679017 - 672.39502;
                rect.height = coords[3] - coords[1];
                //rect.width = 390.577109 - 125.824906;
                rect.width = coords[2] - coords[0];;
                
                
                //rect.x = 125.824906;
                rect.x = coords[0];
                //rect.y = pageHeight -672.39502 - rect.height; 
                rect.y = pageHeight -coords[1] - rect.height;
                System.out.println(rect);
                
                stripper.addRegion( "class1", rect );
                stripper.extractRegions( firstPage );
                
                
                System.out.println( "Text in the area:" + rect );
                System.out.println( stripper.getTextForRegion( "class1" ) );

            }
            finally
            {
                if( document != null )
                {
                    document.close();
                }
            }
        }
    }

    /**
     * This will print the usage for this document.
     */
    private static void usage()
    {
        System.err.println( "Usage: java org.apache.pdfbox.examples.util.ExtractTextByArea <input-pdf>" );
    }

}

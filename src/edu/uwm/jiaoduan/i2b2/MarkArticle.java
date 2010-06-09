package edu.uwm.jiaoduan.i2b2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class MarkArticle {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 2)
			System.out
					.println("usage:\t<goldFile> <txtFile>\n\t<txtFile> <goldFile1> <goldFile2> ...");
		else if (args[0].equals("folder")) {
			String textFolder = args[1];
			for (int i = 2; i < args.length; i++) {
				generateMarkedFolder(args[i], textFolder);
			}

		} else if (args.length == 2) {
			String goldfile = args[0];
			String textfile = args[1];
			String outfile = "gold.html";
			generateMarkedHTML(goldfile, textfile, outfile);
		} else if (args.length > 2) {
			String textfile = args[0];
			for (int i = 1; i < args.length; i++) {
				generateMarkedHTML(args[i], textfile, args[i] + ".html");
			}
		}

		System.out.println("over");

	}

	private static void generateMarkedFolder(String goldFolder,
			String textFolder) {
		try {
			File tf = new File(textFolder);
			for (File f : tf.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.indexOf(".") < 0;
				}
			})) {
				generateMarkedHTML(goldFolder + "/" + f.getName()
						+ ".i2b2.entries", f.getCanonicalPath(), goldFolder
						+ "/" + f.getName() + ".html");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void generateMarkedHTML(String goldfile, String textfile,
			String outfile) {
		try {
			ListedMedication lm = new ListedMedication(textfile);

			ArrayList<String> lines = RawInput.getListByEachLine(goldfile, false);

			String taggedArticle = lm.getFieldTaggedText(lines);

			BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
			out.write("<html><head>");
//			out.write("<link rel='stylesheet' type='text/css' href='i2b2.css' />");
			out.write("<style type='text/css'>");
			out.write("m                            ");
			out.write("{                            ");
			out.write("text-align:center;           ");
			out.write("color:red;                   ");
			out.write("font-family:bold;            ");
			out.write("text-transform:uppercase;    ");
            out.write("                             ");
			out.write("}                            ");
			out.write("list                         ");
			out.write("{                            ");
            out.write("                             ");
			out.write("background-color: blue       ");
			out.write("}                            ");
			out.write("do                           ");
			out.write("{                            ");
			out.write("text-align:center;           ");
			out.write("color:blue;                  ");
			out.write("font-family:bold;            ");
			out.write("text-transform:uppercase;    ");
			out.write("background-color: #FFFF66    ");
			out.write("}                            ");
			out.write("f                            ");
			out.write("{                            ");
			out.write("text-align:center;           ");
			out.write("color:blue;                  ");
			out.write("font-family:bold;            ");
			out.write("text-transform:uppercase;    ");
			out.write("background-color: green      ");
			out.write("}                            ");
			out.write("mo                           ");
			out.write("{                            ");
			out.write("text-align:center;           ");
			out.write("color:read;                  ");
			out.write("font-family:bold;            ");
			out.write("text-transform:uppercase;    ");
			out.write("background-color: blue       ");
			out.write("}                            ");
			out.write("r                            ");
			out.write("{                            ");
			out.write("text-align:center;           ");
			out.write("color:#888888;                  ");
			out.write("font-family:bold;            ");
			out.write("text-transform:uppercase;    ");
			out.write("background-color: blue       ");
			out.write("}                            ");
			out.write("</style>");
			out.write("</head><body><pre>");
			out.write(taggedArticle);
			out.write("</pre></body></html>Legend:<br/><span class='r'>Reason</span><span class='mo'>mo</span>");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

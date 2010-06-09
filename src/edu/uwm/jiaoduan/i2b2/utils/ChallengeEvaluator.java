package edu.uwm.jiaoduan.i2b2.utils;

import java.io.File;
import java.io.IOException;

import org.python.core.PyBoolean;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import edu.uwm.jiaoduan.Messages;

public class ChallengeEvaluator {

	private PythonInterpreter interp = null;

	public ChallengeEvaluator() {
		// TODO Auto-generated constructor stub
		PySystemState.initialize();
		interp = new PythonInterpreter();

		interp.exec("import sys");

		String python_path = System.getenv("JYTHON_HOME");
		python_path += "\\lib";
		String AddLibSearchPath = "sys.path.append('" + python_path + "')";
		interp.exec(AddLibSearchPath);

		System.out.println(python_path);

		String i2b2EvalScriptFolder = Messages
				.getString("i2b2.evaluation.python.script.folder");
		String sciptName = Messages
				.getString("i2b2.evaluation.python.script.filename");
		System.out.println(i2b2EvalScriptFolder);

		AddLibSearchPath = "sys.path.append('" + i2b2EvalScriptFolder + "')";
		System.out.println(AddLibSearchPath);
		interp.exec(AddLibSearchPath);

		// interp.execfile(i2b2EvalScriptFolder + sciptName);
		interp.exec("import math,re,sets,os");

		interp.exec("import i2b2eval, i2b2obj");
		interp.exec("import re");
		interp.exec("from i2b2eval import InformativeMention");
		interp.exec("from i2b2eval import VectorDocument");
		interp.exec("from i2b2eval import Match");
		interp.exec("from i2b2eval import MatchingScheme");

		interp.exec("from i2b2obj import Mention");
		interp.exec("from i2b2obj import MentionTuple");
		interp.exec("from i2b2obj import MentionTupleParser");

		interp.exec("from optparse import OptionParser");

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
//		String i2b2EvalScriptFolder = edu.uwm.jiaoduan.Messages
//				.getString("i2b2.evaluation.python.script.folder");
//		String sciptName = Messages
//				.getString("i2b2.evaluation.python.script.name");
//		System.out.println(i2b2EvalScriptFolder);

		ChallengeEvaluator evaluator = new ChallengeEvaluator();
		
//		evaluator.i2b2Challenge2009GoldStandardTest();
		evaluator.i2b2TestZuofengAnnotation();
	}

	public void i2b2Challenge2009GoldStandardTest() throws IOException {
		PyString recordPath = new PyString(Messages
				.getString("i2b2.challenge.competition.data.folder") 
				+ "2/");

		String root = Messages
		.getString("i2b2.evaluation.python.script.folder");
		
//		Build gold standard xml file
		PyString entriesGoldPath = new PyString(Messages
				.getString("i2b2.Gold.Entries.folder"));
		
		File outfile = new File(root + "gold.xml");
		String gfile = outfile.getCanonicalPath();

		PyString GoldXmlOut = new PyString(gfile);
		this.BuildBatchFiles(recordPath, entriesGoldPath, GoldXmlOut);
		
//		build JMerki XML file
		String JMerkiFolder = Messages.getString("i2b2.challenge.system.output.jmerki.folder") 
				+ "2/";
		PyString entriesJMeriPath = new PyString(JMerkiFolder);
		
		outfile = new File(root + "jmerki.xml");
		String sysfile = outfile.getCanonicalPath();
		PyString jmerkiXmlOut = new PyString(sysfile);
		this.BuildBatchFiles(recordPath, entriesJMeriPath, jmerkiXmlOut);
		
//		Lancet147 XML file
		String LancetFolder = Messages.getString("i2b2.challenge.system.output.crf.folder")
				+ "2/";
		PyString entriesLancetFolder = new PyString(LancetFolder);
		
		outfile = new File(root + "lancet.xml");
		sysfile = outfile.getCanonicalPath();
		PyString lancetXmlOut = new PyString(sysfile);
		this.BuildBatchFiles(recordPath, entriesLancetFolder, lancetXmlOut);

//		test Jmerki
		System.out.println("JMerki");
		I2b2Evaluator ie = new I2b2Evaluator();
		ie.test(entriesGoldPath.toString(), JMerkiFolder , System.out);
		System.out.println("Lancet147");
		ie.test(entriesGoldPath.toString(), LancetFolder, System.out);
		

	}
	
	private void i2b2TestZuofengAnnotation() throws IOException {
		PyString recordPath = new PyString(Messages
				.getString("i2b2.challenge.competition.data.folder")
				+ "2/");

		PyString entriesGoldPath = new PyString(Messages
				.getString("i2b2.Gold.Entries.folder"));

		String root = Messages
		.getString("i2b2.evaluation.python.script.folder");

		File outfile = new File(root + "gold.xml");
		String gfile = outfile.getCanonicalPath();

		PyString GoldXmlOut = new PyString(gfile);
		BuildBatchFiles(recordPath, entriesGoldPath, GoldXmlOut);
		//		end of build gold standard xml file.

		outfile = new File(root + "zuofeng17.xml");
		String sysfile = outfile.getCanonicalPath();
		PyString zfAnnotXmlOut = new PyString(sysfile);


		PyString entriesZFAnnotPath = new PyString(Messages
				.getString("JiaoDuan.i2b2.Zuofeng.Annotation.17.folder"));
		BuildBatchFiles(recordPath, entriesZFAnnotPath, zfAnnotXmlOut);

		I2b2Evaluator ie = new I2b2Evaluator();
				//		ie.test(Messages
//				.getString("i2b2.Gold.Entries.folder"), Messages
//				.getString("JiaoDuan.i2b2.Zuofeng.Annotation.17.folder"), System.out);
		Evaluate(GoldXmlOut, zfAnnotXmlOut);

	}

	public void SingleArticleEvaluation(String sysoutPath) throws IOException {

		File sysfile = new File(sysoutPath);
		String filename = sysfile.getName();
		String nameroot = filename.replaceFirst(".i2b2.entries", "");
		File recordfile = new File(Messages.getString("i2b2.TrainDataFolder")
				+ "/2/" + nameroot);

		File goldfile = new File(Messages.getString("i2b2.Gold.Entries.folder")
				+ "/2/" + filename);

		File folder = new File(Messages
				.getString("i2b2.single.evaluation.root"));
		String fullPath = "";
		try {
			fullPath = folder.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		RawInput rin = new RawInput();
		rin.delFolder(fullPath);

		if (!folder.mkdir())
			System.err
					.println("Erro in Challenge2009: 1157 am: create folder error.");

		File sysFolder = new File(fullPath + "/sys");
		if (!sysFolder.mkdir())
			System.err
					.println("Erro in Challenge2009 sys : 1157 am: create folder error.");

		File recordFolder = new File(fullPath + "/record");
		if (!recordFolder.mkdir())
			System.err
					.println("Erro in Challenge2009 record: 1157 am: create folder error.");

		File goldFolder = new File(fullPath + "/gold");
		if (!goldFolder.mkdir())
			System.err
					.println("Erro in Challenge2009 gold: 1157 am: create folder error.");

		File dstSysfile = new File(sysFolder.getCanonicalFile() + "/"
				+ filename);
		File dstGoldfile = new File(goldFolder.getCanonicalFile() + "/"
				+ filename);
		File dstRecordfile = new File(recordFolder.getCanonicalFile() + "/"
				+ nameroot);
		rin.copyfile(sysfile, dstSysfile);
		rin.copyfile(goldfile, dstGoldfile);
		rin.copyfile(recordfile, dstRecordfile);

		// evaluation
		String i2b2EvalScriptFolder = edu.uwm.jiaoduan.Messages
				.getString("i2b2.evaluation.python.script.folder");
		String sciptName = Messages
				.getString("i2b2.evaluation.python.script.name");
		System.out.println(i2b2EvalScriptFolder);

		PyString recordPath = new PyString(recordFolder.getCanonicalPath()
				+ "/");

		PyString goldPath = new PyString(goldFolder.getCanonicalPath() + "/");

		String root = Messages
				.getString("i2b2.evaluation.python.script.folder");

		File outfile = new File(root + "gold.xml");
		String gfile = outfile.getCanonicalPath();
		PyString GoldXmlOut = new PyString(gfile);

		BuildBatchFiles(recordPath, goldPath, GoldXmlOut);

		outfile = new File(root + "article.xml");
		String sysoutfile = outfile.getCanonicalPath();
		PyString articleXmlOut = new PyString(sysoutfile);

		PyString sysPath = new PyString(sysFolder.getCanonicalPath() + "/");
		BuildBatchFiles(recordPath, sysPath, articleXmlOut);

		// PyString CRFXmlOut = new PyString(root + "crfOut.xml");
		// entriesPath = new
		// PyString(Messages.getString("i2b2.challenge.system.output.crf.folder")
		// + "2/");
		// evaluator.BuildBatchFiles(recordPath, entriesPath, CRFXmlOut);
		//        
		// System.out.println(CRFXmlOut);

		// evaluation

		Evaluate(GoldXmlOut, articleXmlOut);

	}

	private void Evaluate(PyString goldXmlOut, PyString systemXmlOut) {
		// interp.set("recordPath", recordPath);

		// interp.eval("print '\t'.join(['#in/exact','vert/horiz','sys/pat','tag/X','fmeasure'])");

		interp.set("goldXml", goldXmlOut);
		interp.set("systXml", systemXmlOut);
		interp.exec("print goldXml");
		interp.exec("print systXml");
		interp.exec("i2b2eval.Evaluate(goldXml, systXml, False)");
		interp.exec("i2b2eval.Evaluate(goldXml, systXml, True)");
	}

	private void BuildBatchFiles(PyString recordPath, PyString entriesPath,
			PyString xmlOut) {
		// TODO Auto-generated method stub

		interp.set("recordpath", recordPath);
		interp.set("entrypath", entriesPath);
		interp.set("xmlout", xmlOut);
		interp.exec("print recordpath");
//		interp.exec("elist = [1,5,3,7]");
//		interp.exec("print sorted(elist, reverse=True)");

		interp
				.exec("i2b2eval.build_batch_files(recordpath, entrypath, xmlout)");

	}

}

package edu.uwm.jiaoduan.i2b2.utils;

//for splitta
import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import edu.uwm.jiaoduan.Messages;
import org.python.modules.cPickle;


public class Jsplitta {

	private static final int NB_MODEL = 0;
	private static final int SVM_MODEL = 1;
	private ArrayList<String> tmpfiles = new ArrayList<String>();
	private String svmModel_tmp;
	private String svmClassifer_tmp;
	
	/**
	 * @param args
	 */
	static PythonInterpreter interp = null;
	
	public Jsplitta(int model_type) {
		try {
			if(model_type != SVM_MODEL)
				model_type = NB_MODEL;
			initSplitta(model_type);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initSplitta(int model_type) throws IOException {
		PySystemState.initialize();
		interp  = new PythonInterpreter();
    	
//    	interp.exec("import re, cPickle, os, sys, math");
    	interp.exec("import jdsbd");
//    	interp.exec("import sys; print sys.path");

//    	interp.exec("from jdsbd import Model");
    	
    	String splitta_path = Jsplitta.class.getResource("/splitta").getPath();
    	System.out.println(splitta_path);
    	File folder = new File(splitta_path);

    	
    	
    	String mlpath = "/model_nb/";
    	if(model_type == SVM_MODEL)
    	{
    		mlpath = "/model_svm/";
    		
    		URL svmModel = Jsplitta.class.getResource(mlpath + "svm_model");
    		svmModel_tmp = RawInput.getTemporaryFilePath(svmModel.openStream());

    		
        	if(System.getProperty("os.name").indexOf("Windows") >=0){
        		URL svmClassifer = Jsplitta.class.getResource("/svm_light/svm_classify.exe");
        		svmClassifer_tmp = RawInput.getTemporaryFilePath(svmClassifer.openStream(),"exe");
        	}else{
        		URL svmClassifer = Jsplitta.class.getResource("/svm_light/svm_classify");
        		svmClassifer_tmp = RawInput.getTemporaryFilePath(svmClassifer.openStream());
        	}
    		
    		interp.set("use_svm", true);
    	}else
    		interp.set("use_svm", false);
    	interp.exec("print use_svm");
    	URL feats = Jsplitta.class.getResource(mlpath + "feats");
        URL lowords = Jsplitta.class.getResource(mlpath + "lower_words");
    	URL non_abbrs = Jsplitta.class.getResource(mlpath + "non_abbrs");
    	
    	String feats_tmp = RawInput.getTemporaryFilePath(feats.openStream());
    	interp.set("f", feats_tmp);
    	tmpfiles.add(feats_tmp);
    	
    	String lowords_tmp = RawInput.getTemporaryFilePath(lowords.openStream());
    	interp.set("l", lowords_tmp);
    	tmpfiles.add(lowords_tmp);
    	
    	String non_abbrs_tmp = RawInput.getTemporaryFilePath(non_abbrs.openStream());
    	interp.set("n", non_abbrs_tmp); 
    	tmpfiles.add(non_abbrs_tmp);
    	
    	interp.set("model_path", mlpath);

    	String loadmodel = "model = jdsbd.load_sbd_model_from_file(model_path, use_svm, f, l, n)";
    	System.out.println("Loading Splitta...");
    	
    	interp.exec(loadmodel);
    	
    	clean();
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
    	
    	Jsplitta jsp = new Jsplitta(1);
    	
		
//		InitSplitta(interp, NB_MODEL);
		boolean token = true;
		String content = "Motrin 400 mg q.8h. p.r.n. pain , NovoLog 24 units subq q.a.c. , \nLantus 60 units subcutaneous q.10 p.m. , Toprol-XL";
		String[] result = jsp.RunSplitta(content, true);
		for(String line: result)
			System.out.println(line);

	}

	public String[] RunSplitta(String content, boolean token) throws IOException {   	
		content += "\n\n";
		

		
//		"./sample.txt";
		String sampleFile = RawInput.getTemporaryFilePath("lancet","spt");
		BufferedWriter ftmp = new BufferedWriter(new FileWriter(sampleFile));
		ftmp.write(content);
		ftmp.close();
		tmpfiles.add(sampleFile);
		
		String sentences = new String();
		PyString pySampleFile = new PyString(sampleFile);
		interp.set("sample", pySampleFile);
//		test = sbd.get_data_from_string('sample.txt', tokenize=False)
		String get_data = "test = jdsbd.get_data(sample, tokenize=False)";
		
		if(token)
		{
			get_data = get_data.replace("=False", "=True");
		}
		
		interp.exec(get_data);
//		interp.exec("print model");
		interp.exec("test.featurize(model)");
		
//		interp.exec("global svm_model_file");
		interp.set("svm_model_file", svmModel_tmp);
		interp.set("svm_light_classifier", svmClassifer_tmp);
		interp.exec("model.set_svm_model_file(svm_model_file, svm_light_classifier)");
		

		interp.exec("model.classify(test)");
		
		String outFile = RawInput.getTemporaryFilePath("lancet","spt");
		tmpfiles.add(outFile);
		
		interp.set("outfile", new PyString(outFile));
		interp.exec("f = open(outfile, 'w')");
		interp.exec("test.segment(use_preds=True, tokenize=False, output= f)");
		interp.exec("f.close()");
		
		ArrayList<String> lineList = RawInput.getListByEachLine(outFile, false);
		String[] lines = new String[lineList.size()];
		for(int i =0; i < lineList.size(); i ++)
			lines[i] = lineList.get(i);
		
		clean();
		
		return lines; 
	}

	private void clean() {
		RawInput.delAllFiles(tmpfiles);
		tmpfiles.clear();
	}
}

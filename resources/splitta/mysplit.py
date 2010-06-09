import sbd, util,word_tokenize,os
from sbd import Model
from sbd import NB_Model
from sbd import SVM_Model
from util import Counter
from sbd import Frag
from sbd import Doc


model_path = './splitta/model_svm/';
model = sbd.load_sbd_model(model_path)

test = sbd.get_data("./splitta/sample.txt", tokenize=False)
test.featurize(model)
model.classify(test)
outfile = "123.txt";
f = open(outfile, 'w')

test.segment(use_preds=True, tokenize=False, output=f)
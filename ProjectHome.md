Lancet is a supervised machine-learning system that automatically extracts medication events consisting of medication names and information pertaining to their prescribed use (dosage, mode, frequency, duration and reason) from lists or narrative text in medical discharge summaries.

<img src='http://lh3.ggpht.com/_hGh4_43C1gk/SujHB6qvTPI/AAAAAAAAApU/X6EuE81KkmM/s1280/i2b2-entry.jpg' height='250' width='700' />


Lancet applies a conditional random fields model for tagging individual medication names and associated fields, and an AdaBoost model with decision stump algorithm for determining which medication names and fields belong to a single medication event.

During the  third i2b2 shared-task for challenges in natural language processing for clinical data: medication extraction challenge, lancet is ranked as a Top10 system. Among the top 10 teams, Lancet achieved the highest precision at 90.4% with an overall F1 score of 76.4% (horizontal system level with exact match), a gain of 11.2% and 12%, respectively, compared with the rule-based baseline system jMerki.

This is the source code for the paper **"Lancet: a high precision medication event extraction system for clinical text"** published in [Journal of the American Medical Informatics Association 2010, 17:563 -567](http://jamia.bmj.com/content/17/5/563.abstract)

Code written in Java and Python.

You could evaluate Lancet at my online demo at:/Not available now./
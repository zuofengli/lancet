#!/usr/bin/perl -w
=copyright
    Copyright 2007 Sigfried Gold
    This file is part of MERKI.  MERKI is free software: you can redistribute it and/or modify it 
    under the terms of the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with this program.  
    If not, see <http://www.gnu.org/licenses/>.
=cut
use strict;
use ParseMeds;
use YAML::Syck;
use Data::Dumper;
# Fake data (from fake WebCIS patients and further de-identified)
my $text = q(
- "he patient was discharged on azithromycin 250 mg p.o. for two days, ASiPiLin 81mg p.o., prednisone 40 mg p.o. for one day, continued on Flovent 110 mcg 2 puffs b.i.d. with a spacer and albuterol meter dose inhaler 2 q. i.d. p.r.n."
);
my $dsums = Load($text);
my $parser = ParseMeds->new(); 

for my $dsum (@$dsums) {
    my $drugs = $parser->twoLevelParse($dsum, ['drug', 'possibleDrug', 'context'], ['dose', 'route', 'freq', 'prn', 'date']);	
    print "==  Extracting drugs  ============================================\n";
    print $dsum, "\n";
    print "------------------------------\n";
    print $parser->drugsToXML($drugs);
    print "\n\n";
}

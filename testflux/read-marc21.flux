// opens file 'fileName', interprets the content as marc21 and writes to stdout

default fileName = FLUX_DIR + "10.marc21";
//default file = FLUX_DIR + "/test.txt";
default dir2read = FLUX_DIR + "/files";


dir2read|
read-dir|

open-exfile|
as-lines|
decode-marc21|
encode-formeta(style="multiline") |
write("stdout");
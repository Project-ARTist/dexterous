#!/usr/bin/env python3

import datetime
import zipfile

test_zip_filename = 'de.wetteronline.wetterapp_injected.apk'

with zipfile.ZipFile(test_zip_filename, 'r') as myzip:
    test_result = myzip.testzip()
    if (test_result is None) :
        print('OK    : File: ' + test_zip_filename + " is Healthy.")
        for zipcontent in myzip.infolist() :
            print(zipcontent.filename)
            print('\tComment:\t'        + str(zipcontent.comment.decode("utf-8")))
            print('\tModified:\t'       + str(datetime.datetime(*zipcontent.date_time)))
            print('\tZIP version:\t'    + str(zipcontent.create_version))
            print('\tCompressed:\t'     + str(zipcontent.compress_size) + ' bytes')
            print('\tUncompressed:\t'   + str(zipcontent.file_size) + ' bytes')
    else:
        print("ERROR : First Bad Filename: " + str(test_result) + " in " + test_zip_filename)
    if (test_result is None) :
        print('\n# Summary:')
        print('OK    : File: ' + test_zip_filename + " is Healthy.")

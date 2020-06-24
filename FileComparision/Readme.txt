The program takes 3 arguments
argument-1 : path to the old set of files.
argument-2 : path to the new set of files.
argument-3 : path to save the output.

pre-Requisite: create 2 directories under the path mentioned in argument-3.
			Directoris to be created :: CHANGED_INC_FILES & Deployables
Output:
	1.In the path provided as argument-3, a file will be created with the name "filesToBeDeployed_<timeStamp>.txt".
		This file contains the copy commands of the list of files that are different. 
		Once the CMDs in the file are run the files that are different are copied to the Deployables directory.
	2.In the CHANGED_INC_FILES directory created, the deletes and inserts of that file are copied to a new file with the same INC file name.
		If the job is run more than once and the files that are different exists in the CHANGED_INC_FILES directory,
		the existing file is renamed with "<fileName>_<TimeStamp>".
		
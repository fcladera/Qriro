/*
This file contains some useful code examples used during the development of the project
*/


#if 0
	// How to use gsl_matrices
	gsl_matrix *A,*B, *C, *D, *E;
	A = gsl_matrix_calloc(2,2);
	B = gsl_matrix_calloc(2,2);
	C = gsl_matrix_calloc(2,2);
	D = gsl_matrix_calloc(2,1);
	E = gsl_matrix_calloc(2,1);
	gsl_matrix_set(A,0,0,1);
	gsl_matrix_set(A,0,1,2);
	gsl_matrix_set(A,1,0,3);
	gsl_matrix_set(A,1,1,4);


	gsl_matrix_set(B,0,0,5);
	gsl_matrix_set(B,0,1,6);
	gsl_matrix_set(B,1,0,7);
	gsl_matrix_set(B,1,1,8);

	gsl_matrix_set(D,0,0,3);
	gsl_matrix_set(D,1,0,7);

	//gsl_matrix_fprintf(stdout,A,"%g");
	printMatrix(A);
	printMatrix(B);
	//gsl_matrix_fprintf(stdout,B,"%g");
	gsl_blas_dgemm(CblasNoTrans,CblasNoTrans,
						1.0, A,B,
						0.0, C);
		printMatrix(C);
	gsl_blas_dgemm(CblasNoTrans,CblasNoTrans,
						1.0, A,D,
						0.0, E);
	printMatrix(E);


	gsl_matrix_free(A);
	gsl_matrix_free(B);
	gsl_matrix_free(C);
	gsl_matrix_free(D);
	gsl_matrix_free(E);

	return 0;

#endif

#if 0
	// This is an example how to fetch multiple lines from a char[]
	FILE *fd = NULL;
	fd = fopen("example.tst","r");
	if (fd==NULL) perror(__FILE__);
	char linea[MAX_REC_LEN];
	printf("File opened\n");
	/*
	while (!feof(fd)) {
	  double gyroValues[3];
	  double timeValue;
	  int scanresult = fscanf(fd,"%*c:%lf:%lf:%lf:%lf;\n",&timeValue,gyroValues,gyroValues+1,gyroValues+2);
	  printf("%d\n",scanresult);
	  if( scanresult != 4){
		  printf("EOF?\n");
		  break;
	  }

	  //printf("Line %lf:%lf:%lf:%lf\n", timeValue,gyroValues[0],gyroValues[1],gyroValues[2]);
	}
	*/
	fread(linea,1,MAX_REC_LEN,fd);
	fclose(fd);
	//printf("%s",linea);

	char * glissant = linea;
	while (*glissant!='\0') {
		  double gyroValues[3];
		  double timeValue;
		  int scanresult = sscanf(glissant,"%*c:%lf:%lf:%lf:%lf;\n",&timeValue,gyroValues,gyroValues+1,gyroValues+2);
		  printf("%d\n",scanresult);
		  if( scanresult != 4){
			  printf("EOF?\n");
			  break;
		  }

		  printf("Line %lf:%lf:%lf:%lf\n", timeValue,gyroValues[0],gyroValues[1],gyroValues[2]);

		  // jump to next line
		  while(*(++glissant)!='\n');
		  glissant++;

	}
	return 0;
	#endif


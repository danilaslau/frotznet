#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static void *readfile(const char *infile, int *sz)
{
	FILE *fp;
	int  size;
	char *data;
	
	fp = fopen(infile, "rb");
	if(fp == 0) {
		return 0;
	}
	fseek(fp, 0, SEEK_END);
	size = ftell(fp);
	rewind(fp);
	
	data = (char *)malloc(size + 2);
	fread(data, size, 1, fp);
	fclose(fp);
	
	if(sz != 0) *sz = size;
	return (void*) data;
}

#define KEYMAX 32

int START(int x)
{
	if((x >= 'a') && (x <= 'z')) return 1;
	if((x >= 'A') && (x <= 'Z')) return 1;
	if(x > 0x3000) return 1;
	return 0;
}

int CONT(int x)
{
	if((x >= 'a') && (x <= 'z')) return 1;
	if((x >= 'A') && (x <= 'Z')) return 1;
	if(x > 255) return 1;
	if(x == '-') return 1;
	if(x == '.') return 1;
	if((x >= '0') && (x <= '9')) return 1;
	return 0;
}



void write_utf8(FILE *fp, const unsigned short *ptr, int len)
{
	while(len-- > 0){
		unsigned short c = *ptr++;
			
		if(c < 0x0080) {
			fputc(c,fp);
		} else if(c < 0x0800) {
			fputc(0xc0 | (c >> 6),fp);
			fputc(0x80 | (c & 0x3f),fp);
		} else {
			fputc(0xe0 | (c >> 12),fp);
			fputc(0x80 | ((c >> 6) & 0x3f),fp);
			fputc(0x80 | (c & 0x3f),fp);
		}
	}
}


void EJECT(unsigned short *x)
{
	unsigned short *p;
	p = x + 1;

	while((*p >= ' ') && (*p != ')') && (*p != '[') && (*p != ']') && (*p != ',') && (*p != '/')) p++;

	
	write_utf8(stdout, x, p - x);
	printf("\n");
}


unsigned short *data;

int COMPARE(const void *A, const void *B)
{
	int a, b, i;
	
	unsigned short *aptr = data + *((unsigned int*) A);
	unsigned short *bptr = data + *((unsigned int*) B);

	for(i = 0; i < KEYMAX; i++){
		a = *(aptr + i);
		b = *(bptr + i);

		if((a >= 0x30a1) && (a <= 0x30f4)) a -= 0x60;
		if((b >= 0x30a1) && (b <= 0x30f4)) b -= 0x60;
		
		if((a >= 'A') && (a <= 'Z')) a |= 0x20;
		if((b >= 'A') && (b <= 'Z')) b |= 0x20;
		
		if(a != b) break;
	}

	return (a - b);
}


const char *EXCLUDE[] = {
	"abbr", "col", "vul", "vulg", "then", "and", "conj", "int", "pron",
	"suf", "syn", "ant", "from", "adv", "adj", "hon", "hum", "pol",
	"adj-na", "adj-no", "adj-pn", "v5aru", "v5b", "v5g", "v5k", "v5k-s",
	"v5m", "v5r", "v5s", "v5t", "v5u", "etc",
	0
};

int fixup(dsz)
{
	int i,j;
	int first = 1;
	
	for(i = 0, j = 0; i < dsz; i++){
		if(first){
			if(data[i] == '/'){
				first = 0;
				continue;
			}
		}
		if(data[i+1] == '\n'){
			if(data[i] == '/'){
				first = 1;
				continue;
			}
		}
		data[j++] = data[i];
	}
	return j;
}

int main(int argc, char **argv)
{
	unsigned short key[KEYMAX + 1];
	char name[128];
	unsigned int *idx;
	unsigned int c;
	int i, dsz, isz, p, start;
	int verbose = 1;
	FILE *fp;
	
	data = (unsigned short *) readfile(argv[1],&dsz);
	dsz /= 2;

	printf("initial size = %d\n",dsz);
	dsz = fixup(dsz);
	printf("adjusted size = %d\n",dsz);
	
	
	if(data == 0) return -1;

	idx = (unsigned int*) malloc(sizeof(int) * 1000000);
	isz = 0;

//	printf("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
//	printf("<pre>\n");
	
	p = 0;
	start = 0;
	for(i = 0; i < dsz; i++){
		c = data[i];
		if(p == 0){
			if(START(c)){
				key[0] = c;
				p = 1;
				start = i;
			}
		} else {
			if(CONT(c)){
				key[p] = c;
				if(p < KEYMAX) p++;
			} else {
				if(key[0] < 128) {
					if(p > 2) {
						int j;
						for(j = 0; EXCLUDE[j]; j++){
							if(strlen(EXCLUDE[j]) == p){
								int k;
								for(k = 0; k < p; k++){
									if(EXCLUDE[j][k] != key[k]) goto nomatch;
								}
								goto excluded;
							}
						nomatch:
							;
						}
						idx[isz++] = start;
					excluded:
						;
					}
				} else {
					idx[isz++] = start;
#if 0
					write_utf8(stdout,data + start, p);
					printf("\n");
#endif
				}
				p = 0;
			}
		}
	}

	fprintf(stderr,"entries %d\n",isz);

	qsort(idx, isz, sizeof(unsigned int), COMPARE);


	if(verbose) {
		for(i = 0; i < isz; i++){
			printf("%05d: @%08d ",i,idx[i]);
			EJECT(data + idx[i]);
		}
	}

	sprintf(name,"%s.idx",argv[1]);
	fp = fopen(name,"wb");
	fwrite(idx, sizeof(int), isz, fp);
	fclose(fp);
	
	sprintf(name,"%s.dat",argv[1]);
	fp = fopen(name,"wb");
	fwrite(data, sizeof(short), dsz, fp);
	fclose(fp);
	
	return 0;
	
}

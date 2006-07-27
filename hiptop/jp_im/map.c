
/* cheesy tool to 'compile' a kana map */

#include <stdio.h>

typedef struct rec rec;

struct rec
{
	rec *next;
	rec *prev;
	unsigned int key;
	int a, b, c;
};

rec *list;

void insert(unsigned int key, int a, int b, int c)
{
	rec *r = (rec*) malloc(sizeof(rec));
	rec *x;
	
	r->key = key;
	r->a = a;
	r->b = b;
	r->c = c;

	x = list->next;
	while(x->key > key) x = x->next;
	r->next = x;
	r->prev = x->prev;
	x->prev->next = r;
	x->prev = r;	
}

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
	
	data = (char *)malloc(size);
	fread(data, size, 1, fp);
	fclose(fp);
	
	if(sz != 0) *sz = size;
	return (void*) data;
}

int main(int argc, char *argv[])
{
	int i, j, z;
	unsigned int key,a,b,c;
	unsigned short *data;
	int sz;
	rec *r;

	data = (unsigned short*) readfile("kana.u16", &sz);
	if(data == 0) return -1;
	
	list = (rec*) malloc(sizeof(rec));
	list->next = list;
	list->prev = list;
	list->key = 0;

	z = 1;
	sz /= 2;
	for(i = 0; i < sz; i++){
//		printf("%d\n",z++);
		if(data[i] == '#'){
			while((i < sz) && (data[i] != '\n')) i++;
			continue;
		}
		a = data[i++];
		b = 0;
		c = 0;
		if(data[i] > ' '){
			b = data[i++];
			if(data[i] > ' '){
				c = data[i++];
			}
		}
		if(data[i++] != ' '){
			fprintf(stderr,"oops!\n");
			exit(1);
		}
		key = 0;
		j = 0;
		while(data[i] > ' '){
			key = (key << 8) | data[i];
			i++;
			j++;
		}
		if(j > 4) {
			fprintf(stderr,"OOPS\n");
			exit(1);
		}
		insert(key,a,b,c);
	}
#if 0	
	for(i = 0; KMAP[i].str; i++){
		key = 0;
		for(j = 0; KMAP[i].str[j]; j++){
			key = (key << 8) + KMAP[i].str[j];
		}
		insert(key, KMAP[i].a, KMAP[i].b, KMAP[i].c);
	}
#endif
	
	for(r = list->next; r != list; r = r->next){
#if 0
		printf("0x%08x, 0x%04x, 0x%04x, 0x%04x,\n",r->key,r->a,r->b,r->c);
#else
		printf("%c%c%c%c%c%c%c%c%c%c",
			   r->key & 0xff, (r->key >> 8) & 0xff,
			   (r->key >> 16) & 0xff, (r->key >> 24) & 0xff,
			   r->a & 0xff, (r->a >> 8) & 0xff,
			   r->b & 0xff, (r->b >> 8) & 0xff,
			   r->c & 0xff, (r->c >> 8) & 0xff);
#endif
	}
	return 0;
}

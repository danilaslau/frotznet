## Kernel Functions ##

sceKernelGetSystemTimeWide() returns time in microseconds.

## Graphics Functions ##

sceGuCopyImage() takes about 1630 usec to transfer a 256x256x4 texture:
```
t0 = sceKernelGetSystemTimeWide();
sceGuStart(GU_DIRECT,list);
sceGuCopyImage(GU_PSM_8888,
               0, 0, 256, 256, 256, texture,
               0, 0, 256, texbuf);
sceGuTexSync();
sceGuTexFlush();
sceGuFinish();
sceGuSync(0,0);
t1 = sceKernelGetSystemTimeWide();
```

sceGuCopyImage() seems to stall until the previous one completes if you
issue a second one (doing the sceGuCopyImage() twice above doubles the total time).

Not sure how much impact this has on other rendering or ram access.
## Installing a Vblank handler ##
(thanks tyranid)

```
sceKernelRegisterSubIntrHandler(PSP_VBLANK_INT, 1, vblank_handler, NULL);
sceKernelEnableSubIntr(PSP_VBLANK_INT, 1);
```
# ImgProc

This project demonstrates the effects of quantization on JPEG image

### Steps in the process:
1)	Load the image and display in original quality.
2)	Select compression (quantization) quality.
3)	Adjust quantization matrices Y and UV, depending on quality selected.
4)	Convert RGB into YUV + Subsample chromaticity (U,V channels) by the 4:2:0 ratio
5)	 Apply Discrete Cosine Transform (DCT-II)
6)	Quantize Y channel by the Y Quantization matrix
7)	Quantize U and V channels by the UV Quantization matrix.
8)	De-quantize Y channel by the Y Quantization matrix.
9)	Apply Inverse Discrete Cosine Transform (DCT-II)
10)	Convert YUV420 to RGB
11)	Display compressed quality image

### Program Details

1) Simple GUI allows user to load and display an image, using File explorer. Once loaded, it is displayed in its original
   quality in 512x512 image frame.  User can select compression quality and process the image. 
2) When Process button is clicked, the following program logic is executed:

  a) The system recalculates Y and UV quantization matrices. The scaling factor depends on the quality selected (1-100). The larger quality value selected, the lower value is stored in the matrix, with a minimal value of 1. The following formula is used: Scaling factor = (quality < 50) ? (5000/quality) : (200 - 2 * quality);
     
  b) The RGB values for each pixel are converted to YUV color space. The frame is also sub-sampled at this time by the 4:2:0    ratio. It means that only every other column and row is considered when storing U and V values for the given pixel. The Y, U and V values are stored in 3 separate 2D arrays. The Y array has the same size as the original image frame. The sub-sampled  U, V frames are ½ of the Image size.
I have devised a program logic in which each 8x8 block is visited only once and DCT, Quantization, De-quantization and IDCT functions are all executed before moving to another block.  The goal was to minimize the amount tables needed to store the intermediate results of each function.

  c) The Discrete Cosine Transform (DCT-II) procedure uses the DCT matrix and transforms each 8x8 block into frequency domain. The tables Y, U, and V are processed separately.  This program uses the nested loop to execute DCT. It means 64 loops are needed for each 8x8 block.  Attempts to optimize the code by separating the loops into rows and columns to achieve 16 loops were unsuccessful.
  
  d) The Quantization depends on channel  type. For the Luma (Y) table the Y quantization matrix is used. For the Chroma (U,V), UV matrix is used.
  
  e) De-quantization reverses the calculation process of quantization. It uses the matrices again, depending on Y, U, V tables. It is during this process, that the image is losing its quality and the pixel may appear “choppy” is low quality is selected. 
  f) The Inverse Discrete Cosine Transform (DCT-II) uses the same DCT matrix and restores the Y, U, V values.
  g) The YUV420 to RGB conversion is processing the data by 4x4 pixel blocks. This is needed to recover from the gaps caused by the 4:2:0 subsampling and rebuild the adjusted RGB values for each pixel of the original image. 
The function loops thru the block, where each block contains 4 Y values, but only 1 U and V value. It uses the separate Y values for each block and Y, U values from the first pixel to calculate the RGB values for this block. The results are stored in BufferedImage container.

  h)  Once processed, the image is displayed in separate image frame and user can compare the quality loss, depending on settings.

### Features
-	Image size auto-correction: The program checks the image width and size and ensures that the image can be sub-divided into 8x8 blocks without any remaining pixels.

-	Quantization quality: User can select the quantization quality in the range 1-100, which 50 set as default. 

-	8x8 block values reporting: The program displays the values of 8x8 block in GUI, to demonstrate how the values change during the process of transformation.

### TODO:
- [ ] Implement the process indicator (Please Wait, processing ... )
- [ ] Reimplement the program in C++ and Qt, use Pointers for the Block processing loops (way faster) 

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgproc;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import java.text.DecimalFormat;

/**
 *
 * @author ovalek
 */
//------------------------------------------------------------------------------
public class ImgProc {
    
    BufferedImage wcopyImg;                                                     // working copy of the image    
    
    ImageProcessingUI ui;

    private int imgWidth;
    private int imgHeight;
    
    int N = 8;                                                                  // image processing block size (default 8x8)
    private int quality;                                                        // quantization quality  (1-100), deault = 50
    
    double [][] c;                                                              // DCT coefficient matrix                                                              
    int [][] Y_QMatrix;                                                         // Luma quantization 8x8 matrix   
    int [][] UV_QMatrix;
    
    int qm_sf;                                                                      // quatization table quality scaling factor
    
    int VIEW_X = 120;
    int VIEW_Y = 120;        
    
    //-----------------------
    int [][] YTable;
    int [][] UTable;
    int [][] VTable;
    
    String y_before_dct;
    String y_after_dct;
    String y_after_q;
    String y_after_deq;
    String y_after_idct;
    
    
    int a, R, G, B, 
           Y, U, V;
    
    // -------------------------------------------------------------------------
    // Constructor
    // PARAM:   prcBuffer - working copy of the buffer,
    //          q - quantization quality
    //--------------------------------------------------------------------------
    public ImgProc(BufferedImage prcBuffer, int q){
                                                                                   
        imgWidth = prcBuffer.getWidth();                                        // Image width
        imgHeight = prcBuffer.getHeight();                                      // Image height
        quality = q;                                                            // Quantization quality
   
        // validate against pictures with wrong size
        if ( (imgWidth % 8) != 0 || (imgWidth/2 %8) != 0)       {                                   
            do{                              
                imgWidth--;            
            }while ((imgWidth%8) != 0 || ((imgWidth/2) % 8) != 0);

            System.out.print("\nInproper image width detected, resized to: " + imgWidth);      
        }
                  
        if ( (imgHeight % 8) != 0 || (imgHeight/2 %8) != 0){                                                                      
            do{              
                imgHeight--;            
            }while ((imgHeight % 8) != 0 || (imgHeight/2 %8) != 0);
                    
            System.out.print("\nInproper image height detected, resized to: " + imgHeight);                        
        } 
                             
        wcopyImg = new BufferedImage(imgWidth, imgHeight, TYPE_INT_ARGB);
        wcopyImg = copyImage(prcBuffer, imgWidth, imgHeight);

        //----------------------------------------
        YTable = new int[imgWidth][imgHeight];                                             
        UTable = new int[imgWidth/2][imgHeight/2];                                             // size already set for Subsampling 4:2:0
        VTable = new int[imgWidth/2][imgHeight/2];                                             // --//-- 
        
        /*
        YTable_dct = new double[imgWidth][imgHeight];                                          //use to store results of DCT
        UTable_dct = new double[imgWidth/2][imgHeight/2];
        VTable_dct = new double[imgWidth/2][imgHeight/2];
        */
        
        init_dctMatrix();                                                       // load DCT coefficients

        // Luma quantization matrix Q50 - base
        Y_QMatrix = new int[][] { {16,   11,   10,   16,   24,   40,   51,   61},         
                                  {12,   12,   14,   19,   26,   58,   60,   55},
                                  {14,   13,   16,   24,   40,   57,   69,   56},
                                  {14,   17,   22,   29,   51,   87,   80,   62},
                                  {18,   22,   37,   56,   68,  109,  103,   77},
                                  {24,   35,   55,   64,   81,  104,  113,   92},       
                                  {49,   64,   78,   87,  103,  121,  120,  101},     
                                  {72,   92,   95,   98,  112,  100,  103,   99} };      
        
        // Chroma quantization matrix Q50 base
        UV_QMatrix = new int[][] { {17, 18, 24, 47, 99, 99, 99, 99},
                                   {18, 21, 26, 66, 99, 99, 99, 99},
                                   {24, 26, 56, 99, 99, 99, 99, 99},
                                   {47, 66, 99, 99, 99, 99, 99, 99},
                                   {99, 99, 99, 99, 99, 99, 99, 99},
                                   {99, 99, 99, 99, 99, 99, 99, 99},        
                                   {99, 99, 99, 99, 99, 99, 99, 99},        
                                   {99, 99, 99, 99, 99, 99, 99, 99} };                  
        calculate_qMatrices();        
    }
      
    /**
     *      *  POST: Initializes the DCT matrix  
     */
    //--------------------------------------------------------------------------
    public void init_dctMatrix(){                
               
        c = new double[N][N];                                                   // DCT coefficient matrix 
        final double value = 1/Math.sqrt(2.0);

        for (int i = 1; i < N; i++) {
            for (int j = 1; j < N; j++)
        	c[i][j]=1;        	
        }

        for (int i=0;i<N;i++) {
                c[i][0]= value;
        	c[0][i]= value;
        }
        c[0][0]=0.5;
    }
        
    //--------------------------------------------------------------------------
    // PARAM:   q - quality (1-100)
    // POST:    Update the quantization quality
    //--------------------------------------------------------------------------
    public void setQuality(int q){
        quality = q;
        calculate_qMatrices();
    }
    
    //--------------------------------------------------------------------------
    // POST: Calculates Quantization matrices based on quality settings
    //--------------------------------------------------------------------------
    public void calculate_qMatrices(){
    
        qm_sf = (quality < 50) ? (5000/quality) : (200 - 2 * quality);          // scaling factor
        
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Y_QMatrix[i][j] = (qm_sf * Y_QMatrix[i][j] + 50) / 100 ;
                
                if (Y_QMatrix[i][j] < 1)
                    Y_QMatrix[i][j] = 1;
                                
                UV_QMatrix[i][j] = (qm_sf * UV_QMatrix[i][j] + 50) / 100 ;
                
                if (UV_QMatrix[i][j] < 1)
                    UV_QMatrix[i][j] = 1;                              
            }
        } 
    }
    
    //--------------------------------------------------------------------------
    // POST: The main driver for compression
    //--------------------------------------------------------------------------
    public BufferedImage compressImage(){

        conv_frame_RGBtoYUV420();                                               // 4:2:0 Subsampling included
                
        transform(YTable, imgWidth, imgHeight, Y_QMatrix);        
        transform(UTable, imgWidth/2, imgHeight/2, UV_QMatrix);       
        transform(VTable, imgWidth/2, imgHeight/2, UV_QMatrix);
         
        conv_YUV420_RGB();
        
        return wcopyImg;            
    }
    
    //--------------------------------------------------------------------------
    // POST:    1)Separates R, G, B values for each pixel in the image frame
    //          2) Converts the R, G, B values into Y, U, V color space
    //          3) Subsamples Chroma (U,V) by the 4:2:0 ratio
    //          4) Creates Y, U, V Tables to be used as an input for further processing
    //--------------------------------------------------------------------------
    public void conv_frame_RGBtoYUV420(){
          
        /* // Enable this part, if byte stream is needed
        final int frameSize = imgWidth  * imgHeight;
        final int chromaSize = frameSize / 4;

        // YUV420_data indexes:                                                 // For 256x256 pixels image:
        int yIndex = 0;                                                         // Y values start at index 0         
        int uIndex = frameSize;                                                 // V values start at index 65,536         
        int vIndex = frameSize + chromaSize;                                    // V values start at index 81,920                
        YUV420_data = new byte [ imgWidth * imgHeight * 3/2  ];                  // 4:2:0 = 3/2 coefficient       
        */
                
        int pixel;
                 
        int index = 0;
               
        for ( int j = 0; j < imgHeight; j++ ){                                          
            for (int i = 0; i < imgWidth; i++ ){
                                          
                pixel = wcopyImg.getRGB(i, j);
           
                // int alpha = (pixel >> 24) & 0xff;                            // Enable, if alpha needed                        
                R = (pixel >> 16) & 0xff;                                       
                G = (pixel >> 8) & 0xff;
                B = (pixel) & 0xff;
                                
                // Convert to YUV values                               
                Y = (int) ( R * 0.299    + G *  0.587   + B * 0.114 );
                U = (int) ( R * -0.14713 + G * -0.28886 + B *  0.436 );
                V = (int) ( R *  0.615   + G * -0.51499 + B * -0.10001 );                 
                                             
                YTable[i][j] = Y;
                // YUV420_data[yIndex++] = (byte) Y;                            // enable for Data stream                                                    

                //4:2:0 sub-sampling                
                if (j % 2 == 0 && index % 2 == 0) {
                    
                    UTable[i/2][j/2] = U;                                       
                    VTable [i/2][j/2] = V;                    
                //    YUV420_data[uIndex++] = (byte) U;                         // enable for Data stream                            
                //    YUV420_data[vIndex++] = (byte) V;                         //
                }
                index ++;
            }
        }        
    }
    

    //--------------------------------------------------------------------------
    // POST:    Rebuilds the Image Buffer with compressed RGB values          
    //--------------------------------------------------------------------------
    public void conv_YUV420_RGB(){
                    
        int RGB_value = 0;
               
        for ( int j = 0; j < imgHeight; j += 2 ){                                                                     
            for (int i = 0; i < imgWidth; i += 2 ){

                int tempU = 0, tempV = 0;
                
                // --- Loop thru 2x2 pixel block
                for (int m = 0; m < 4; m++) {
                    
                    // recover from sub-sampling                    
                    switch(m){
                        case 0:     Y = YTable[i][j];       
                                    U = tempU = UTable[i/2][j/2];
                                    V = tempV = VTable[i/2][j/2];                                
                                    break;
                                                                                                                     
                        case 1:     Y = YTable[i+1][j];
                                    U = tempU;
                                    V = tempV;                        
                                    break;
                                                                                                
                        case 2:     Y = YTable[i][j+1];                        
                                    U = tempU;
                                    V = tempV;                                    
                                    break;
                        
                        case 3:     Y = YTable[i+1][j+1];                        
                                    U = tempU;
                                    V = tempV;  
                                    break;
                                    
                        default:    break;       
                    }
  
                    R = (int) (Y + 1.13983 * V);                                
                    G = (int) ((Y - 0.39465 * U - 0.58060 * V));                  
                    B = (int) ((Y + 2.032 * U));

                    // normalize for boundaries                    
                    R = ((R < 0) ? 0 : ((R > 255) ? 255 : R));
                    G = ((G < 0) ? 0 : ((G > 255) ? 255 : G));
                    B = ((B < 0) ? 0 : ((B > 255) ? 255 : B));
                    
                    // Bit shift to comstruct the RGB value
                    RGB_value =  (0xFF & 255) << 24 | (0xff & R) << 16 | ((0xff & G) << 8) | (0xFF & B);        // alpha = 255
                    
                    switch(m){
                        case 0:     wcopyImg.setRGB(i, j,     RGB_value); break;
                        
                        case 1:     wcopyImg.setRGB(i+1, j,   RGB_value); break;
                                                                                   
                        case 2:     wcopyImg.setRGB(i, j+1,   RGB_value); break;  
                                                       
                        case 3:     wcopyImg.setRGB(i+1, j+1, RGB_value); break;
                                                    
                        default:    break;       
                    }  
                }     
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // POST:    Y, U and V Tables are compressed
    // PARAM:   table - Y, U or V table, w - frame width, h - frame height, mx - quantization matrix type (Y or UV)
    //--------------------------------------------------------------------------
    public void transform(int [][] table, int width, int height, int[][] mx){
            
        int [][] i_wcopy_block = new int [N][N];					
        double [][] d_wcopy_block = new double[N][N];
                        
        //---- LOOP thru Image (jump by Image blocks)
	for (int j = 0; j < height; j += 8){            
            for (int i = 0; i < width; i += 8){
                
                //---- COPY image block                
                for (int v = j; v < (j + 8); v++) {                                                                                    
	            for (int u = i; u < (i + 8); u++) {
                        
                        i_wcopy_block[u % 8][v % 8] = table[u][v];                                    
                    }
                }
                                
                if (i == VIEW_X && j == VIEW_Y && width == imgWidth ){                   
                    y_before_dct = loadIntBlock(i_wcopy_block);
                }

                d_wcopy_block = apply_DCT( i_wcopy_block );                     
                                
                if (i == VIEW_X && j == VIEW_Y && width == imgWidth ){                           
                    y_after_dct = loadDoubleBlock(d_wcopy_block);
                }
                           
                i_wcopy_block = quantize( d_wcopy_block, mx );
                
                if (i == VIEW_X && j == VIEW_Y && width == imgWidth )
                    y_after_q = loadIntBlock(i_wcopy_block);
                                                                                                
                d_wcopy_block = dequantize( i_wcopy_block, mx);
                
                if (i == VIEW_X && j == VIEW_Y && width == imgWidth )                                    
                    y_after_deq = loadDoubleBlock(d_wcopy_block);                  
                                
                i_wcopy_block = apply_IDCT( d_wcopy_block);
                
                if (i == VIEW_X && j == VIEW_Y && width == imgWidth )                                
                    y_after_idct = loadIntBlock(i_wcopy_block);

                // LOAD results back to image
                for (int  v = j; v < (j + 8); v++) {                                                                        
	            for (int u = i; u < (i + 8); u++) {  						
                        table[u][v] = i_wcopy_block[u % 8][v % 8 ];                                                
                    }
                }                
            }
        }
                
    }
                                   
    //--------------------------------------------------------------------------
    // POST:    Discrete Cosine Transform performed on 8x8 pixel block
    // PARAM:   block - 8x8 pixel raster
    //--------------------------------------------------------------------------
    public double [][] apply_DCT(int [][] block){
         
        double [][] result = new double [N][N];       
        double temp;
                
        for (int v = 0; v < N; v++) {                                           // Loop thru the Block                  
            for (int u = 0; u < N; u++) {                                                              
                
                        temp = 0.0;                                             
                                              
                        for (int x = 0; x < N; x++) {                           // Accumulation loop                            
                            for (int y = 0; y < N; y++) {                                
                                temp += block[x][y] * Math.cos((( 2*x + 1) /(2.0 * N))* u * Math.PI ) 
                                                    * Math.cos((( 2*y + 1) /(2.0 * N))* v * Math.PI );                                
                            }
                        }                        
                        temp *=  c[u][v]/4.0;            
                        result [u][v] = temp;
            }
        }                        
        return result;
    }
    
    //--------------------------------------------------------------------------
    // POST:    Quantization performed on 8x8 pixel block
    // PARAM:   block - 8x8 raster, matrix - Quantization matrix type (Y or UV)
    //--------------------------------------------------------------------------
    public int[][] quantize(double [][]block, int matrix[][]){
        
        int result [][] = new int [N][N];
        
        double temp; 
        
        for (int v = 0; v < N; v++) {                                           // Loop thru the 8x8 Block                  
            for (int u = 0; u < N; u++) {
                temp = block[u][v] / matrix[u][v];                
                result[u][v] = (int) Math.round(temp);
            }
        }       
        return result;
    }
    
    //--------------------------------------------------------------------------
    // POST:    De-quantization performed on the 8x8 block
    // PARAM:   block - 8x8 raster, matrix - Quantization matrix type (Y or UV)
    //--------------------------------------------------------------------------
    public double[][] dequantize(int [][]block, int matrix[][]){
        
        double result [][] = new double[N][N];
        
        for (int v = 0; v < N; v++) {                                                         
            for (int u = 0; u < N; u++) {                                                      
                result [u][v] = block [u][v] * matrix[u][v];
            }
        }
        return result;
    }
    
    //--------------------------------------------------------------------------
    // POST:    Inverse Discrete Cosine Transform performed on 8x8 pixel block
    // PARAM:   block - 8x8 pixel raster
    //-------------------------------------------------------------------------- 
    public int[][] apply_IDCT( double[][] block ) {
        
       int[][] result = new int[N][N];

        for ( int x = 0; x < N; x++ ) {                                         // run thru raster
            for ( int y = 0; y < N; y++ ) {
            
                double sum = 0.0;
          
                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {                               // Accumulate
          
                        sum += c[u][v] * block[u][v] * Math.cos((( 2 * x + 1) / ( 2.0 * N ) ) * u * Math.PI) 
                                                     * Math.cos((( 2 * y + 1) / ( 2.0 * N ) ) * v * Math.PI);
                    }
                }          
                sum /= 4.0;         
                result[x][y] = (int) Math.round( sum );
            }
        }
       return result;
    }
    
    
    //--------------------------------------------------------------------------
    // HELPER FUNCTIONS
    //--------------------------------------------------------------------------
        
    //--------------------------------------------------------------------------
    // POST:    Creates a copy of BufferedImage
    // PARAM:   source - source BufferedImage
    //--------------------------------------------------------------------------
    public BufferedImage copyImage(BufferedImage source, int w, int h){
        
        BufferedImage b = new BufferedImage(w, h, source.getType());

        Graphics g = b.getGraphics();
        
        g.drawImage(source, 0, 0, w, h, null);

        g.dispose();

        return b;
    }
    
    
    public String getB_before_DCT(){    
        return y_before_dct;
    }
    
    public String getB_after_DCT(){    
        return y_after_dct;
    }
    
    public String getB_after_Q(){    
        return y_after_q;
    }
    
    public String getB_after_DEQ(){    
        return y_after_deq;
    }
    
    public String getB_after_IDCT(){    
        return y_after_idct;
    }
    
    
    
    
    //--------------------------------------------------------------------------
    // DEBUG FUNCTIONS
    //--------------------------------------------------------------------------    
    public void print_QMatrix(){
        
        System.out.print("\n\n Luma (Y) - QMatrix, Quality: " + quality + "  \n\n\t");
 
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t");
            
                System.out.print(Y_QMatrix[i][j] + "\t");            
            }        
        }

        System.out.print("\n\n Chroma (UV) - QMatrix, Quality: " + quality + "  \n\n\t");
        
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t");
            
                System.out.print(UV_QMatrix[i][j] + "\t");            
            }        
        }
        System.out.print("\n\n--------------------------------------------------");
    }
    
    
    
    public String loadIntBlock(int block[][]){

        String result = new String();
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
 
                result += Integer.toString(block[i][j]) + "   ";
                
                if ( i == 7 )
                    result += "\n";
                                          
            }        
        }    
        return result;
    }
    
        public String loadDoubleBlock(double block[][]){

        String result = new String();
        
        DecimalFormat twoDForm = new DecimalFormat("####.##"); 
        
        
        
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                result +=  Double.toString( Double.valueOf( twoDForm.format( block[i][j] ) ) )  + "   ";
                
                
                
                
                
                if ( i == 7 )
                    result += "\n";
                                          
            }        
        }    
        return result;
    }
    
    
    
     //-------------------------------------------------------------------------- DEBUG
    public void printIntBlock(int block [][], String name){
                
        System.out.print("\n\n" + name + "\n\t");
        
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t");
            
                System.out.print((block[i][j] + "\t"));            
            }        
        }
    }
    
    //--------------------------------------------------------------------------
    public void printDoubleBlock( double block[][], String name){
    
        System.out.print("\n\n" + name + "\n\t");
        
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t");
            
                System.out.print(( String.format( "%.2f", block[i][j]) + "\t"));            
            }        
        }
    }
                
    //-------------------------------------------------------------------------- DEBUG
    public void printRGBInt(String name){

        System.out.print("\n\n" + name + "\n\t");
        
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t\t");
            
                System.out.print(" " + wcopyImg.getRGB(i, j) + "\t");            
            }        
        }
    }
        
    //-------------------------------------------------------------------------- DEBUG
    public void printRGBvalue(String name){
        
        int pixel = 0;
        
        System.out.print("\n\n" + name + "\n\t");
        
        for (int i = 0; i < 8; i++)
            System.out.print("[" + i + "]\t");

        System.out.print("\n");
        
        for (int j = 0; j < 8; j++){                      
            for (int i=0; i < 8; i++){
                               
                if (i % 8 == 0)
                    System.out.print("\n[" + j + "]\t\t");
         
                pixel = wcopyImg.getRGB(i, j);
                
                // int alpha = (pixel >> 24) & 0xff;                            // Not used, for reference only                        
                R = (pixel >> 16) & 0xff;                                       
                G = (pixel >> 8) & 0xff;
                B = (pixel) & 0xff;

                System.out.print(" " + R + "," + G + "," + B + "\t");            
            }        
        }
    } 
}
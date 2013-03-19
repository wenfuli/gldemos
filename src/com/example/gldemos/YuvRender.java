package com.example.gldemos;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.example.gldemos.utils.RawResourceReader;
import com.example.gldemos.utils.Snippet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

public class YuvRender implements GLSurfaceView.Renderer {
	private Context context;
	private int mPositionHandle;
	private int mColorHandle;
	private int uniformY;
	private int uniformU;
	private int uniformV;
	private int rectVerts;
	private int rectInds;
	
	private byte[] yuvdatas;
	private Bitmap bitmap;

	public YuvRender(Context context) {
		this.context = context;
		InputStream is = this.context.getResources().openRawResource(
				R.drawable.ic_lesson_two);
		bitmap = BitmapFactory.decodeStream(is);
		yuvdatas = Snippet.getNV21(bitmap.getWidth(), bitmap.getHeight(),
				bitmap);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
		final String vertexShader = "attribute vec4 position;    				\n"
				+ "attribute vec2 texCoord;										\n"

				+ "varying vec2 texCoordVarying;								\n"

				+ "void main()						\n" + "{					\n"
				+ "    gl_Position = position;		\n"
				+ "    texCoordVarying = texCoord;	\n" + "}					\n";

		final String fragmentShader = RawResourceReader.readTextFileFromRawResource(context,
				R.raw.simplefragment);
		// Load in the vertex shader.加载顶点渲染器
		int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		if (vertexShaderHandle != 0) {
			// Pass in the shader source.
			GLES20.glShaderSource(vertexShaderHandle, vertexShader);
			// Compile the shader.
			GLES20.glCompileShader(vertexShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS,
					compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) {
				GLES20.glDeleteShader(vertexShaderHandle);
				vertexShaderHandle = 0;
			}
		}
		if (vertexShaderHandle == 0) {
			throw new RuntimeException("Error creating vertex shader.");
		}
		// Load in the fragment shader shader.加载片段渲染器
		int fragmentShaderHandle = GLES20
				.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

		if (fragmentShaderHandle != 0) {
			// Pass in the shader source.
			GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

			// Compile the shader.
			GLES20.glCompileShader(fragmentShaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(fragmentShaderHandle,
					GLES20.GL_COMPILE_STATUS, compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) {
				GLES20.glDeleteShader(fragmentShaderHandle);
				fragmentShaderHandle = 0;
			}
		}

		if (fragmentShaderHandle == 0) {
			throw new RuntimeException("Error creating fragment shader.");
		}

		// Create a program object and store the handle to it.创建工程对象和存放handle
		int programHandle = GLES20.glCreateProgram();

		if (programHandle != 0) {
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind 
			GLES20.glBindAttribLocation(programHandle, 0, "position");
			GLES20.glBindAttribLocation(programHandle, 1, "texCoord");

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
					linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) {
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		if (programHandle == 0) {
			throw new RuntimeException("Error creating program.");
		}
		GLES20.glUseProgram(programHandle);
		
		mPositionHandle = GLES20.glGetAttribLocation(programHandle,
				"position");
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		mColorHandle = GLES20.glGetAttribLocation(programHandle, "texCoord");
		GLES20.glEnableVertexAttribArray(mColorHandle);
		uniformY = GLES20.glGetUniformLocation(programHandle, "SamplerY");
		uniformU = GLES20.glGetUniformLocation(programHandle, "SamplerU");
		uniformV = GLES20.glGetUniformLocation(programHandle, "SamplerV");
		GLES20.glUniform1i(uniformY, 0);
		GLES20.glUniform1i(uniformU, 1);
		GLES20.glUniform1i(uniformV, 2);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClearColor( 0.5f, 0.2f, 0.4f, 1.f );
		float[] mvertices = 
			   {    
				    1f, -1f, 0f, // V1
					1f, 1f, // Texture coordinate for V1

					1f, 1f, 0f, // V2
					1f, 0f,

					-1f, 1f, 0f, // V3
					0f, 0f,

					-1f, -1f, 0f, // V4
					0f, 1f
				};
			FloatBuffer floatBuffer = ByteBuffer.allocateDirect(mvertices.length * 4)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			floatBuffer.put(mvertices).position(0);
			
			byte[] Indices = { 0, 1, 2, 2, 3, 0 };
			int[] buffers = new int[1];
			rectVerts = buffers[0];
			GLES20.glGenBuffers(1, buffers, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, rectVerts);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floatBuffer.capacity()*4,
					floatBuffer, GLES20.GL_STATIC_DRAW);
			
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Indices.length * 2)
					.order(ByteOrder.nativeOrder());
			byteBuffer.put(Indices).position(0);
			final int[] mbuffers = new int[1];
			rectInds = mbuffers[0];
			GLES20.glGenBuffers(1, mbuffers, 0);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, rectInds);
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,byteBuffer.capacity()*1, byteBuffer, GLES20.GL_STATIC_DRAW);
			GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT,
					false, 20, 0);
			GLES20.glVertexAttribPointer(mColorHandle, 2, GLES20.GL_FLOAT, false,
					20, 4 * 3);
		
	    int len = yuvdatas.length;
	    int yLen = (len*2)/3;
	    byte[] pY = new byte[yLen];
	    for(int i=0;i<yLen;i++){
	    	pY[i]=yuvdatas[i];
	    }
	    
	    int uLen = len/6;
	    byte[] pU = new byte[uLen];
	    for(int i=0;i<uLen;i++){
	    	pU[i]=yuvdatas[yLen+i];
	    }
	    
	    int vLen = len/6;
	    byte[] pV = new byte[vLen];
	    for(int i=0;i<vLen;i++){
	    	pV[i]=yuvdatas[yLen+uLen+i];
	    }
	    
	    final int[] texture = new int[3] ;
	    GLES20.glGenTextures(3, texture,0) ;
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1) ;
	    
        ByteBuffer yBuffer = ByteBuffer.allocateDirect(pY.length).order(ByteOrder.nativeOrder());
        yBuffer.put(pY).position(0);
        ByteBuffer uBuffer = ByteBuffer.allocateDirect(pU.length).order(ByteOrder.nativeOrder());
        uBuffer.put(pU).position(0);
        ByteBuffer vBuffer = ByteBuffer.allocateDirect(pV.length).order(ByteOrder.nativeOrder());
        vBuffer.put(pU).position(0);
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, texture[0] );          
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer); 
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST) ;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST) ;
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, texture[1] );          
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, bitmap.getWidth()/2, bitmap.getHeight()/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer); 
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST) ;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST) ;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, texture[2] );          
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, bitmap.getWidth()/2, bitmap.getHeight()/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer); 
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST) ;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST) ;
        
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_BYTE, 0);
        
        GLES20.glDeleteBuffers(1, buffers,0) ;
        GLES20.glDeleteBuffers(1, mbuffers,0) ;
	}
}

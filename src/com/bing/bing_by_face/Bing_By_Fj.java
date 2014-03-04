package com.bing.bing_by_face;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.faceplusplus.api.FaceDetecter;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
/**
 * Android 人脸检测与人脸识别例子，利用Face++实现的程序,用请注明出处.
 * @author bing_liu
 *
 */
public class Bing_By_Fj extends Activity implements PictureCallback {

	private Button facejjButton;
	private FrameLayout cameraLayout;
	private SurfaceView cameraView;
	private Camera mCamera;
	private int camera_h,camera_w;
	private Bing_By_Fj bFj;
	private int cameraID;
	private static int angle=0;
	private boolean facedect=false;
	private static final String TAG="Bing_By_Fj";
	private TextView smliarTextView;
	private int take_count=0;
	private boolean firstLogin=false;//第一次登陆标志
	private boolean checkface=false;
	private ImageView take_picture;
	private boolean takpic=false;
	private List<FacePojo> faceList = new ArrayList<FacePojo>();  
	private double smilar=0;
	private boolean faced=false;
	private boolean picture_exe=false;
	private String picture_path=Environment.getExternalStorageDirectory()+"/face++/";
	private Bitmap myface;
	private FaceDetecter mFaceDetecter;
	HttpRequests mRequests;
	ProgressDialog progressDialog;
	private static SharedPreferences userP=null; 
	private String username="";
	
	String face_id1="6d75041ed74cf257f35243edd12bbb6b";
	String face_id2="";
	
	private String group_id="";
	private String group_name="";
	private String person_id="";
	private String person_name="";
	private TextView faceInformation;
	private String race="",sex="",smiling="",age="";
	
	private boolean netState=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bing__by__fj);
		
		
		faceInformation=(TextView)findViewById(R.id.information);
		cameraLayout=(FrameLayout)findViewById(R.id.bing);
		
		smliarTextView=(TextView)findViewById(R.id.smiliar);
		bFj=this;
		
		facejjButton=(Button)findViewById(R.id.facedectbutton);
		facejjButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mCamera = Camera.open(FindFrontCamera()); 
				cameraLayout.addView(new cameraView(bFj, mCamera));
			}
		});
		
		take_picture=(ImageView)findViewById(R.id.takepicture);
		take_picture.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				getNetState();
				
				if (netState) {
					take_picture.setImageResource(R.drawable.light_red_001);
					takpic=true;
				}else {
					Toast.makeText(bFj, "请检查您的网络状态", Toast.LENGTH_LONG).show();
				}
				
			}
		});
		
		mFaceDetecter=new FaceDetecter();
		boolean m=mFaceDetecter.init(bFj, "fa8cecf6a4d667a805b006893a24ffdd");
		Log.e(TAG, "初始化:"+m);
		mRequests=new HttpRequests("fa8cecf6a4d667a805b006893a24ffdd", "-JK8kIekVF8DR7JcgaELC39h4kZIDqoA");
		
		progressDialog=new ProgressDialog(bFj);
		
		LoadFaceData();
		
		init_id();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if (mCamera != null) {
            mCamera.setPreviewCallback(null); 
            mCamera.stopPreview();
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
		filerename();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bing__by__fj, menu);
		return true;
	}
	
	class cameraView extends SurfaceView implements Callback{
		 Size mPreviewSize;
		 List<Size> mSupportedPreviewSizes;
		public cameraView(Context context,Camera camera) {
			super(context);
			// TODO Auto-generated constructor stub
			SurfaceHolder surfaceHolder;
			mCamera=camera;
			surfaceHolder=getHolder();
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			surfaceHolder.addCallback(this);
			
			mSupportedPreviewSizes=mCamera.getParameters().getSupportedPreviewSizes();
		}
		@SuppressLint("NewApi")
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
			 if(holder.getSurface() == null) { return; }
			 mCamera.stopPreview();
			camera_h=height;
			camera_w=width;
			
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
       	Camera.Parameters parameters = mCamera.getParameters();
           parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
           mCamera.setParameters(parameters);
           requestLayout();
           try {
           	setCameraDisplayOrientation(bFj, cameraID, mCamera);
   			mCamera.setPreviewDisplay(holder);
   			mCamera.startPreview();
//   			mCamera.setFaceDetectionListener(new face_Dect());
//   			startFaceDetection();
			} catch (Exception e) {
				// TODO: handle exception
			}			
           camera_h=mPreviewSize.height;
           camera_w=mPreviewSize.width;
          
      	 new Thread(face_recon).start();
           
		}
		private Size getOptimalPreviewSize(List<Size> sizes,
				int width, int height) {
			// TODO Auto-generated method stub
	        final double ASPECT_TOLERANCE = 0.2;
	        double targetRatio = (double) width / height;
	        if (sizes == null) return null;

	        Size optimalSize = null;
	        double minDiff = Double.MAX_VALUE;

	        int targetHeight = height;

	        // Try to find an size match aspect ratio and size
	        for (Size size : sizes) {
	            double ratio = (double) size.width / size.height;
	            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
	            if (Math.abs(size.height - targetHeight) < minDiff) {
	                optimalSize = size;
	                minDiff = Math.abs(size.height - targetHeight);
	            }
	        }

	        // Cannot find the one match the aspect ratio, ignore the requirement
	        if (optimalSize == null) {
	            minDiff = Double.MAX_VALUE;
	            for (Size size : sizes) {
	                if (Math.abs(size.height - targetHeight) < minDiff) {
	                    optimalSize = size;
	                    minDiff = Math.abs(size.height - targetHeight);
	                }
	            }
	        }
	        return optimalSize;
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			
			try {  
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			if (mCamera != null) {
               mCamera.setPreviewCallback(null); 
               mCamera.stopPreview();
               mCamera.release(); // release the camera for other applications
               mCamera = null;
               
               
           }
		}
		
		@Override
       protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
           // We purposely disregard child measurements because act as a
           // wrapper to a SurfaceView that centers the camera preview instead
           // of stretching it.
           final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
           final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
           setMeasuredDimension(width, height);

           if (mSupportedPreviewSizes != null) {
               mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
           }
       }
		
		
		
	}
	
	@SuppressLint("NewApi")
	public static void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info =
	             new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int  cameraCount = Camera.getNumberOfCameras(); // get cameras number  
	     
	     Log.i(TAG, "摄像头数量:"+cameraCount);
	     int rotation = activity.getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     if (cameraCount<2) {
			angle=-result;
		} else {
			 angle=result;
		}
	    
	     camera.setDisplayOrientation(result);
	 }
	
	
	private int FindFrontCamera(){  
        int cameraCount = 0;  
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();  
        cameraCount = Camera.getNumberOfCameras(); // get cameras number  
        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {  
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo  
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {   
            	cameraID=camIdx;
            	return camIdx;
            }  
        }  
        return 0;  
    }  
	
	@SuppressLint("NewApi")
	class face_Dect implements FaceDetectionListener{

		@Override
		public void onFaceDetection(android.hardware.Camera.Face[] faces,
				Camera camera) {
			// TODO Auto-generated method stub
			if (faces.length>0) {
				facedect=true;
//				Log.i(TAG, "检测到人脸");
			}
//			Log.i(TAG, "人脸数量:"+faces.length);
		}
		
	}
	
	@SuppressLint("NewApi")
	public void startFaceDetection() {
	    Camera.Parameters params = mCamera.getParameters();

	    if (params.getMaxNumDetectedFaces() > 0) {
	        mCamera.startFaceDetection();
	    }
	}
	
	
	long netime=0,timesys=0;
	
	/**
	 * 识别线程
	 */
	Runnable face_recon=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
//			firstLogin=false;
			while (!checkface) {
//					timecount++;
					netime=System.currentTimeMillis();
//					Log.i(TAG, "计时");
				timesys=System.currentTimeMillis()-netime;		
//				Log.i(TAG, "时间:"+timesys);
				if (!faced&&/*(facedect||*/takpic) {
					
					
//						Log.i(TAG, "时差:"+timesys);
						mCamera.takePicture(null, null, Bing_By_Fj.this);
						
						take_count++;
						Log.i(TAG, "计数:"+take_count);
					dectPicture();
					if (picture_exe) {
//						face_recognition();
					}
					
					if (smilar>85) {
						faced=true;
						checkface=true;
						Log.i("=========", "登陆成功");
						
						Intent intent=new Intent();
						intent.setClass(bFj, Login.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						finish();
					}
					
					if (!firstLogin&&null!=myface) {
						b2s(myface);
						compare();
						
					}
					if (firstLogin&&null!=myface) {
						b2s(myface);
						setpersonname();
						groupcreate();
						get_id(face_id1, group_id, group_name, person_id, person_name);
						firstLogin=false;
						faced=true;
						
						Intent intent=new Intent();
						intent.setClass(bFj, Login.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						finish();
					}
					
					facedect=false;
					if (take_count==5) {
						takpic=false;
						take_count=0;
					}
				}
				
				Message message=new Message();
				message.what=0;
				login.sendMessage(message);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	};
	
	
	
	
 
	private Handler login=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			int message=msg.what;
			switch (message) {
			case 0:
				if (take_count!=0) {
					take_picture.setImageResource(R.drawable.light1_001);
				}
				smliarTextView.setTextColor(Color.WHITE);
				smliarTextView.setText("相似度:"+smilar);
				faceInformation.setTextColor(Color.WHITE);
				faceInformation.setText("区域:"+race+"\n"
						+"性别:"+sex+"\n"
						+"微笑:"+smiling+"\n"
						+"年龄:"+age);
				
				if (smilar!=0) {
					if (smilar>80) {
//						login();
					} else {
						dialog_nb();
					}
				}
				
				break;

			default:
				break;
			}
			
			
			super.handleMessage(msg);
		}
		
	};
	/**
	 * 创建路径
	 * @param path 路径
	 */
	public static void createPath(String path) {
	    File file = new File(path);
	   if (!file.exists()) {
	       file.mkdir();
	    }
	}
	
	/**
	 * 加载文件
	 */
	 @SuppressLint("SdCardPath")
	public void LoadFaceData() {  
		 
		 	createPath("/sdcard/face++/");
	        File[] files = new File("/sdcard/face++/").listFiles();  
	        File f;  
	        String id;  
	        String name;  
	        String path;
	        FacePojo mPojo = new FacePojo();
	        faceList.clear();  
	        Log.i(TAG, "文件数量"+files.length);
	        if (files.length==0) {
				firstLogin=true;
			}
	        
	        for (int i = 0; i < files.length; i++) {  
	            f = files[i];  
	            if (!f.canRead()) {  
	                return;  
	            }  
	            if (f.isFile()) {  
	                id = f.getName().split("_")[0];  
	                name = f.getName().substring(f.getName().indexOf("_") + 1,  
	                        f.getName().length() - 4);  
	                path=Environment.getExternalStorageDirectory()+"/face++/"+f.getName();
	                if (!name.equals("BING1")) {				
					
	                mPojo.id=id;
	                mPojo.name=name;
	                mPojo.path=Environment.getExternalStorageDirectory()+"/face++/"+f.getName();
	                Log.i(TAG, "名称:"+name);
	                Log.i(TAG, "路径:"+path);
	                faceList.add(mPojo);
	                mPojo=null;
	                mPojo=new FacePojo();
	                }else {
						f.delete();
					}
	            }  
	        }  
	        mPojo=null;
	    }  
	
	 public void filerename(){
			File file = new File("/sdcard/face++/BING1.jpg");  
	        File newFile = new File("/sdcard/face++/bnj.jpg");  
	        File path = new File("/sdcard/face++/");  

	        if(path.exists()){  
	            path.delete();  
	        }  
	        /* 给文件重命名 */  
	        if(file.exists()){  
	            file.renameTo(newFile);  
	        }  
		}

	@Override
	public void onPictureTaken(byte[] arg0, Camera arg1) {
		// TODO Auto-generated method stub
		 Matrix matrix = new Matrix(); 
	        matrix.setRotate(-angle);  
	        
	        int width=0,height=0;
	       
			try {
				Bitmap bitmap = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);  
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),  
			                bitmap.getHeight(), matrix, true); 
				width=bitmap.getWidth();
				height=bitmap.getHeight();
				
//				bitmap=mAlphaFilter.overlay(bitmap, motoBitmap);
//				bitmap=Bitmap.createBitmap(bitmap, width/8, height/9, width/9*7, height/7*6);
				
				bitmap=bitmap.copy(Config.ARGB_8888, true);
				
				myface=bitmap.copy(Config.ARGB_8888, true);
				Log.i(TAG, "myface:"+myface);
				Log.i(TAG, "图片属性:"+bitmap.getConfig());
				if (firstLogin) {
					Log.i(TAG, "第一次登陆");
					progressDialog.setMessage("正在检测");
					progressDialog.show();
					File bing_picture=new File(picture_path,"BI.jpg");
					BufferedOutputStream bos=new BufferedOutputStream(new 
							FileOutputStream(bing_picture));
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
					bos.flush();
					bos.close();
					
				}else {
					progressDialog.setMessage("正在识别");
					progressDialog.show();
					File bing_picture=new File(picture_path,"BING1.jpg");
					BufferedOutputStream bos=new BufferedOutputStream(new 
							FileOutputStream(bing_picture));
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
					bos.flush();
					bos.close();
					Log.i(TAG, "拍照");
				}
				bitmap.recycle();
				bitmap=null;
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
			System.gc();
	        mCamera.startPreview();
	}
	 
	/**
	 * 检测图片是否生成
	 */
	public void dectPicture(){
		  File[] files = new File("/sdcard/face++/").listFiles();  
		  File f;  
		  String bingString;
		  int length=files.length;
		  for (int i = 0; i < length; i++) {
			  f = files[i];  
	            if (!f.canRead()) {  
	                return;  
	            } 
	            
	            bingString = f.getName().substring(f.getName().indexOf("_") + 1,  
                        f.getName().length() - 4); 
	            if (bingString.equals("BING1")) {
					picture_exe=true;
				}
	            
		}
		  
		  f=null;
		  files=null;
	}
	
	public void b2s(Bitmap mBitmap){
		 ByteArrayOutputStream stream = new ByteArrayOutputStream();
        float scale = Math.min(1,
                        Math.min(600f / mBitmap.getWidth(), 600f / mBitmap.getHeight()));
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap imgSmall = Bitmap.createBitmap(mBitmap, 0, 0,
                        mBitmap.getWidth(), mBitmap.getHeight(), matrix, false);

        imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] array = stream.toByteArray();
        JSONObject mString = null;
        
        try {
       	 
			mString=mRequests.detectionDetect(new PostParameters().setImg(array));
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Log.i(TAG, "返回;"+mString);
        try {
        	String cc=mString.getJSONArray("face").getJSONObject(0).getString("face_id");
        	
        	sex=mString.getJSONArray("face").getJSONObject(0).getJSONObject("attribute")
					.getJSONObject("gender").getString("value");
        	
        	smiling=mString.getJSONArray("face").getJSONObject(0).getJSONObject("attribute")
					.getJSONObject("smiling").getString("value");
        	
        	race=mString.getJSONArray("face").getJSONObject(0).getJSONObject("attribute")
					.getJSONObject("race").getString("value");
        	
        	age=mString.getJSONArray("face").getJSONObject(0).getJSONObject("attribute")
					.getJSONObject("age").getString("value");
        	
        	Log.i(TAG, "人中;"+race);
			Log.i(TAG, "性别:"+sex);
			Log.i(TAG, "微笑:"+smiling);
			Log.i(TAG, "年龄:"+age);
        	
			Log.i(TAG, "face_id;"+cc);
			if (!firstLogin) {
				face_id2=cc;
			}else {
				face_id1=cc;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (firstLogin) {
        	 progressDialog.dismiss();
		}
       
	}
	
	/**
	 * 面部对比
	 */
	public void compare(){
		JSONObject mJsonObject=null;
		try {
			mJsonObject=mRequests.recognitionCompare(new PostParameters()
			.setFaceId1(face_id1).
			setFaceId2(face_id2));
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.i(TAG, "比较返回:"+mJsonObject);
		try {
			smilar=Double.valueOf(mJsonObject.getString("similarity"));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		progressDialog.dismiss();
		checkface=true;
	}
	
	/**
	 * 创建person
	 */
	public void setpersonname(){
		JSONObject mJsonObject=null;
		try {
			mJsonObject=mRequests.personCreate(new PostParameters()
			.setFaceId(face_id1).
			setPersonName(username));
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "id返回:"+mJsonObject);
		if (firstLogin) {
			try {
				person_name=mJsonObject.getString("person_name");
				person_id=mJsonObject.getString("person_id");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Log.i(TAG, "person_name:"+person_name);
			Log.i(TAG, "person_id:"+person_id);
		}
	}
	
	/**
	 * 创建组
	 */
	public void groupcreate(){
		JSONObject mJsonObject=null;
		try {
			mJsonObject=mRequests.groupCreate(new PostParameters()
			.setGroupId(username)
			.setPersonName(username));
			
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "组创建:"+mJsonObject);
		
		if (firstLogin) {
			try {
				group_name=mJsonObject.getString("group_name");
				group_id=mJsonObject.getString("group_id");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Log.i(TAG, "group_name:"+group_name);
			Log.i(TAG, "group_id:"+group_id);
		}
		
	}
	
	
	
	public void init_id(){
		userP=this.getSharedPreferences("userinfo", Context.MODE_PRIVATE);
		if (userP.getBoolean("冰", false)) {
			face_id1=userP.getString("face_id1", null);
		}else {
			TelephonyManager telephonyManager=(TelephonyManager)this.
					getSystemService(Context.TELEPHONY_SERVICE);
			username=telephonyManager.getDeviceId()+getSystemtime();
		}
	}
	/**
	 * 
	 * @param face_id 脸部id
	 * @param gro_id 组id
	 * @param gro_name 组名称
	 * @param perS_id 人id
	 * @param per_name 人名
	 */
	public void get_id(String face_id,
			String gro_id,
			String gro_name,
			String perS_id,
			String per_name){
		Editor editor=userP.edit();
		editor.putString("face_id", face_id);
		editor.putString("group_id", gro_id);
		editor.putString("group_name", gro_name);
		editor.putString("person_name", per_name);
		editor.putString("person_id", perS_id);
		userP.getBoolean("冰", true);
		editor.commit();
		
	}
	/**
	 * 登陆
	 */
	public void login(){
		
		Intent intent=new Intent();
		intent.setClass(bFj, Login.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
		
	}
	
	public void dialog_nb(){
		 faced=true;
		 firstLogin=false;
		 LayoutInflater fInflater=LayoutInflater.from(bFj);
		 final View view=fInflater.inflate(R.layout.password, null);
		 final EditText editText=(EditText)view.findViewById(R.id.face_recon);
		 editText.setHint("请输入面部识别码");
		 new AlertDialog.Builder(bFj)
		 .setTitle("登录提示")
		 .setView(view)
		 .setPositiveButton("登陆", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String passdword=editText.getText().toString();
				
				Intent intent=new Intent();
				intent.setClass(bFj, Login.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				finish();
				
			}
		})
		.setNegativeButton("取消", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				fi_Lo_dialog();
				
			}
			
		}).show();
	 }
	
	
	 public void fi_Lo_dialog(){
		 faced=false;
		 new AlertDialog.Builder(bFj )
     	.setTitle("登录提示")
     	.setMessage("初次登陆选项")
     	.setPositiveButton("录入图像", new  DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					Toast.makeText(bFj, "为保证精度请将脸放入圆框中", Toast.LENGTH_LONG).show();
					 firstLogin=true;
				}
				
			})
			.setNegativeButton("取消", new  DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					finish();
				}
				
			}).show();
	 }
	
	 public String getSystemtime(){
		 Calendar calendar=Calendar.getInstance();
			
			String time=calendar.get(Calendar.YEAR)+"_"
					+(calendar.get(Calendar.MONTH)+1)+"_"
					+calendar.get(Calendar.DAY_OF_MONTH)+"_"
					+calendar.get(Calendar.HOUR_OF_DAY)+"_"
					+calendar.get(Calendar.MINUTE);
			
			return time;
	 }
	 
	public void getNetState(){
		ConnectivityManager cManager=(ConnectivityManager)getSystemService(Activity.CONNECTIVITY_SERVICE);
		boolean wifi=cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
		boolean edge=cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
		
		if (wifi||edge) {
			netState=true;
		}
		

	}
}

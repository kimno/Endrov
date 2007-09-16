package evplugin.modelWindow;

import javax.vecmath.*;
import javax.media.opengl.*;

//Vector3D vs javax.vecmath.....? part of javax.media.*

public class Camera
	{
	/** Camera position */
	public Vector3d pos=new Vector3d(0.0,2.0,-15.0);

	/** Center position */
	public Vector3d center=new Vector3d(0,0,0);
	
	/** Transformation matrix */
	private final Matrix3d mat;
	
	

	
	public Camera()
		{
		mat=new Matrix3d();
		mat.setIdentity();
		}
	
	/**
	 * Move camera relative to camera coordinate system
	 */
	public void moveCamera(double x, double y, double z)
		{
		Vector3d vfront=new Vector3d(0,0,z);
		Vector3d vup=new Vector3d(0,y,0);
		Vector3d vright=new Vector3d(x,0,0);
		mat.transform(vfront);
		mat.transform(vup);
		mat.transform(vright);
		pos.add(vfront);
		pos.add(vup);
		pos.add(vright);
		}
	
	/**
	 * Rotate around camera, relative to camera
	 */
	public void rotateCamera(double x, double y, double z)
		{
		Matrix3d matx2=new Matrix3d();	matx2.rotX(-x); //rotation camera up/down
		Matrix3d maty2=new Matrix3d();	maty2.rotY(-y); //rotation camera left/right
		Matrix3d matz2=new Matrix3d();	matz2.rotZ(-z); //rotation around camera axis
		mat.mul(matz2);
		mat.mul(maty2);
		mat.mul(matx2);
		}
	
	
	/**
	 * Set the rotation of the camera
	 */
	public void setRotation(double x, double y, double z)
		{
		mat.setIdentity();
		rotateCamera(x,y,z);
		}

	/**
	 * Do the GL transformation to move into camera coordinates
	 */
	public void transformGL(GL gl)
		{
		mulMatGL(gl, mat);
		gl.glTranslated(-pos.x, -pos.y, -pos.z);
		}
	
	/**
	 * Inverse GL camera rotation
	 */
	public void unrotateGL(GL gl)
		{
		Matrix3d inv=new Matrix3d();
		inv.invert(mat);
		mulMatGL(gl, inv);
		}

	/**
	 * Put a java media matrix on the GL stack
	 */
	private static void mulMatGL(GL gl, Matrix3d mat)
		{
		gl.glMultMatrixd(new double[]{
				mat.m00, mat.m01, mat.m02,0,
				mat.m10, mat.m11, mat.m12,0,
				mat.m20, mat.m21, mat.m22,0,
				0,       0,       0,      1},0);
		}
	
	/**
	 * Transform a point world coord to cam coord
	 */
	private Vector3d transformPoint(Vector3d v)
		{
		Matrix3d inv=new Matrix3d();	inv.invert(mat);
		Vector3d u=new Vector3d(v);		u.sub(pos);
		inv.transform(u);
		return u;
		}

	/**
	 * Rotate camera around center
	 */
	public void rotateCenter(double x, double y, double z)
		{
		Vector3d cameraCenter=transformPoint(center);
		rotateCamera(x, y, z);
		Vector3d cameraCenterNew=transformPoint(center);
		Vector3d diff=new Vector3d();
		diff.sub(cameraCenterNew,cameraCenter);
		moveCamera(diff.x, diff.y, diff.z);
		}
	
	/**
	 * Make camera move at center at some distance with current rotation
	 */
	public void center(double dist)
		{
		Vector3d frontv=new Vector3d(0,0,dist);
		mat.transform(frontv);
		pos.add(frontv, center);
		}
	}
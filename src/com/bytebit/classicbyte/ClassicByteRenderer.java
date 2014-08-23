/*
Ace of Spades remake
Copyright (C) 2014 ByteBit

This program is free software; you can redistribute it and/or modify it under the terms of
the GNU General Public License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program;
if not, see <http://www.gnu.org/licenses/>.
*/


package com.bytebit.classicbyte;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class ClassicByteRenderer implements GLSurfaceView.Renderer {
	public int width = 0;
	public int height = 0;
	
	public Player player = new Player();
	public World world = new World();
	
	public Context context;
	
	public ClassicByteView parent;
	
	public NetworkManager networkManager = new NetworkManager(this);
	
	public ChatManager chatmananger = new ChatManager();
	
	public ArrayList<EntityPlayer> players = new ArrayList<EntityPlayer>();
	public ArrayList<Selection> selections = new ArrayList<Selection>();
	
	public boolean force_chunk_update = false;
	public int force_chunk_update_x = -1;
	public int force_chunk_update_z = -1;
	
	public void setContext(Context c) {
		this.context = c;
	}
	
	public void setView(ClassicByteView v) {
		this.parent = v;
	}
	
	public void onDrawFrame(GL10 gl) {
		long time = System.currentTimeMillis();
		GLES10.glClear(GLES10.GL_DEPTH_BUFFER_BIT | GLES10.GL_COLOR_BUFFER_BIT);
		
		if(ClassicByteView.current_screen==null) {
			GLES10.glMatrixMode(GLES10.GL_PROJECTION);
			GLES10.glLoadIdentity();
			GLU.gluPerspective(gl, 70.0F, (float) this.width / (float) this.height,0.1F,64.0F);
			float eye_pos_x = (float)(this.player.getXPosition()+100*Math.cos(this.player.camera_rot_x) + 100*Math.sin(this.player.camera_rot_x));
			float eye_pos_y = (float)(this.player.getPlayerEyeHeight()+this.player.getYPosition()+100*Math.PI*Math.cos(this.player.camera_rot_y));
			float eye_pos_z = (float)(this.player.getZPosition()+100*Math.sin(this.player.camera_rot_x) - 100*Math.cos(this.player.camera_rot_x));
			
			GLU.gluLookAt(gl, this.player.getXPosition(), this.player.getYPosition()+this.player.getPlayerEyeHeight(), this.player.getZPosition(), eye_pos_x, eye_pos_y, eye_pos_z, 0.0F, 1.0F, 0.0F);
			GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
			TextureManager.bindTexture(1);
			
			int t = RenderChunk.CHUNK_LENGTH_OF_BORDER;
			
			if(ChunkListUpdater.active) {
				while(ChunkListUpdater.active) {}
			}
			
			if(this.force_chunk_update && this.force_chunk_update_x==-1 && this.force_chunk_update_z==-1) {
				ChunkListUpdater.disable_building = true;
				
				Runnable f = new Runnable() {
		            public void run() {
		            	ProgressDialog progress = new ProgressDialog(ClassicByte.view.getContext());
						progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progress.setMessage("Generating chunks...");
						progress.setCancelable(false);
						int a = (int)(ClassicByte.view.renderer.world.z_size/RenderChunk.CHUNK_LENGTH_OF_BORDER+0.5F);
						int b = (int)(ClassicByte.view.renderer.world.x_size/RenderChunk.CHUNK_LENGTH_OF_BORDER+0.5F);
						progress.setMax(a*b);
						progress.show();
						ClassicByte.information_c = progress;
		            }
		        };
				((ClassicByte)ClassicByte.view.getContext()).runOnUiThread(f);
				
				int a = (int)(ClassicByte.view.renderer.world.z_size/t+0.5F);
				int b = (int)(ClassicByte.view.renderer.world.x_size/t+0.5F);
				
				for(int z2=0;z2!=a;z2++) {
					for(int x2=0;x2!=b;x2++) {
						if(this.world.renderchunks[x2][z2]!=null) {
							this.world.renderchunks[x2][z2].buildVBO();
						}
						((ProgressDialog)ClassicByte.information_c).incrementProgressBy(1);
					}
				}
				((ProgressDialog)ClassicByte.information_c).dismiss();
				ChunkListUpdater.disable_building = false;
				this.force_chunk_update = false;
				this.force_chunk_update_x = -1;
				this.force_chunk_update_z = -1;
			}
			
			
			if(this.force_chunk_update && this.force_chunk_update_x!=-1 && this.force_chunk_update_z!=-1) {
				ChunkListUpdater.disable_building = true;
				this.world.renderchunks[this.force_chunk_update_x][this.force_chunk_update_z].buildVBO();
				ChunkListUpdater.disable_building = false;
				this.force_chunk_update = false;
				this.force_chunk_update_x = -1;
				this.force_chunk_update_z = -1;
			}
			
			GLES10.glPushMatrix();
			GLES10.glScalef(0.5F,0.5F,0.5F);
			if(!Options.fancy_graphics) {
				GLES10.glDisable(GLES10.GL_BLEND);
				for(int k=0;k!=ChunkListUpdater.chunks_to_render_size;k++) {
					if(ChunkListUpdater.chunks_to_render[k]!=null) {
						ChunkListUpdater.chunks_to_render[k].renderVBO();
					}
				}
				GLES10.glEnable(GLES10.GL_BLEND);
			} else {
				for(int k=0;k!=ChunkListUpdater.chunks_to_render_size;k++) {
					if(ChunkListUpdater.chunks_to_render[k]!=null) {
						ChunkListUpdater.chunks_to_render[k].renderVBO();
					}
				}
			}
			GLES10.glPopMatrix();
			
			
			
			for(int k=0;k!=this.players.size();k++) {
				if(k>=this.players.size()) {
					break;
				}
				this.players.get(k).render(gl);
			}
			
			
			
			TextureManager.bindTexture(1);
			GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
			GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
			GLES10.glMatrixMode(GLES10.GL_PROJECTION);
			GLES10.glLoadIdentity();
			GLU.gluPerspective(gl, 45.0F, (float) this.width / (float) this.height,0.1F,10.0F);
			GLU.gluLookAt(gl, -5.5F, 0.4F, -1.0F,0.0F,0.0F,0.0F, 0.0F, 1.0F, 0.0F);
			GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
			GLES10.glLoadIdentity();
			GLES10.glDepthRangef(0.0F, 0.1F);
			GLES10.glTranslatef(0.0F,-1.1F,1.0F);
			if(this.player.isMoving()) {
				GLES10.glTranslatef((float)((Math.sin(System.currentTimeMillis()*0.008))*0.1),(float)((Math.sin(System.currentTimeMillis()*0.008)+1.0)*0.5*0.1),0.0F);
			}
			GLES10.glScalef(-1.7F,-1.7F,1.7F);
			IngameRenderer.render3D(gl);
			GLES10.glDepthRangef(0.0F, 1.0F);
			GLES10.glLoadIdentity();
		}
		Canvas c = new Canvas(TextureManager.getBitmap(0));
		c.drawColor(0, Mode.CLEAR);
			    
			    
		if(ClassicByteView.current_screen!=null) {
			ClassicByteView.current_screen.parent = this.parent;
			ClassicByteView.current_screen.draw(c);
		} else {
			IngameRenderer.render2D(c);
		}
			    
			    
		Canvas f = new Canvas(TextureManager.getBitmap(20));
		if(ClassicByteView.current_screen==null) {
			f.drawColor(0, Mode.CLEAR);
		}
		f.drawBitmap(TextureManager.getBitmap(0), 0, 0, ClassicByte.view.standard_paint);
		
		TextureManager.bindTexture(20);
			    
		GLES10.glMatrixMode(GLES10.GL_PROJECTION);
		GLES10.glLoadIdentity();
		GLES10.glOrthof(0, this.width, this.height, 0, -1, 1);
		GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
		GLES10.glLoadIdentity();
		GLES10.glDepthMask(false);
		GLES10.glDisable(GLES10.GL_DEPTH_TEST);

		TextureManager.run(this.width, this.height);
				
		GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
		GLES10.glVertexPointer(2, GLES10.GL_SHORT, 0, TextureManager.screen_overlay_vertex_buffer);
		GLES10.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, TextureManager.screen_overlay_texture_coords_buffer);
		GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
		
		IngameRenderer.render2DSecondPass();
				
		GLES10.glDepthMask(true);
		GLES10.glEnable(GLES10.GL_DEPTH_TEST);
		IngameRenderer.CHAT_STATUS_1 = (char)14+"FPS: "+(1000/(System.currentTimeMillis()-time));
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES10.glViewport(0, 0, width, height);
		this.width = width;
		this.height = height;
	    Bitmap bitm = Bitmap.createBitmap(this.width, this.height, Config.ARGB_8888);
	    TextureManager.loadTextureFromBitmap(bitm,0);
	    
	    int a = this.width;
	    if(this.height>this.width) {
	    	a = this.height;
	    }
	    
	    //find suitable resolution
	    int k = 1;
	    while(k<a) {
	    	k = k * 2;
	    }
	    
	    Logger.log(this, "Texture resolution: "+k+"px");
	    
	    bitm = Bitmap.createBitmap(k, k, Config.ARGB_8888);
	    TextureManager.loadTextureFromBitmap(bitm,20);
	    //rescale bitmaps to correct size in percent
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(2), this.width/10, Math.round(this.width/10*1.1F)), 2);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(3), this.width/10, this.width/10), 3);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(4), Math.round(this.width/10*5.4F), this.width/10), 4);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(5), Math.round(this.width*0.08F), Math.round(this.width*0.08F)), 5);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(6), Math.round(this.width*0.08F), Math.round(this.width*0.08F)), 6);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(7), Math.round(this.width*0.08F*2.5F), Math.round(this.width*0.08F)), 7);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(8), Math.round(this.width*0.08F*2.5F), Math.round(this.width*0.08F)), 8);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(9), Math.round(this.width*0.08F*3.5F), Math.round(this.width*0.08F)), 9);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(10), Math.round(this.width*0.08F*11.4F), Math.round(this.width*0.08F)), 10);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(11), Math.round((this.width*0.08F*11.4F)*0.38F), Math.round((this.width*0.08F*11.4F)*0.38F)), 11);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(13), Math.round(this.width*0.08F*11.4F), Math.round(this.width*0.08F)), 13);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(14), Math.round(this.width*0.08F*11.4F/2.0F), Math.round(this.width*0.08F)), 14);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(15), Math.round(this.width*0.08F*11.4F/2.0F), Math.round(this.width*0.08F)), 15);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(16), Math.round(this.width*0.08F*11.4F/2.0F), Math.round(this.width*0.08F)), 16);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(17), Math.round(this.width*0.08F*11.4F/2.0F), Math.round(this.width*0.08F)), 17);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(18), this.width, this.height), 18);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(19), this.width, this.height), 19);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(21), Math.round(this.width*0.08F*11.4F*0.6F), Math.round((this.width*0.08F*11.4F)*0.38F)), 21);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(22), Math.round(this.width*0.04F), Math.round(this.width*0.04F)), 22);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(23), Math.round(this.width*0.04F), Math.round(this.width*0.04F)), 23);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(24), Math.round(this.width*0.04F), Math.round(this.width*0.04F)), 24);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(25), Math.round(this.width*0.04F), Math.round(this.width*0.04F)), 25);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(26), Math.round(this.width*0.04F), Math.round(this.width*0.08F)), 26);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(27), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 27);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(28), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 28);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(29), Math.round(this.width*0.36F), Math.round(this.width*0.09F)), 29);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(30), Math.round(this.width*0.36F), Math.round(this.width*0.09F)), 30);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(32), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 32);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(33), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 33);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(34), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 34);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(35), Math.round(this.width*0.06F), Math.round(this.width*0.06F)), 35);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(36), Math.round(this.width*0.2F), Math.round(this.width*0.2F)), 36);
		TextureManager.loadTextureFromBitmap(TextureManager.scaleBitmap(TextureManager.getBitmap(37), Math.round(this.width*0.08F), Math.round(this.width*0.08F)), 37);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//this.background_music = MediaPlayer.create(this.parent.getContext(), R.drawable.keygsubtonal);
		//background_music.setLooping(true);
		//background_music.start();
        this.parent.setScreen(new ScreenLogin(this.parent));
		GLES10.glClearColor((156.0F)/256.0F, (205.0F)/256.0F, (255.0F)/256.0F, 1.0F);
		GLES10.glEnable(GLES10.GL_DEPTH_TEST);
		GLES10.glDepthFunc(GLES10.GL_LESS);
		GLES10.glHint(GLES10.GL_PERSPECTIVE_CORRECTION_HINT, GLES10.GL_NICEST);
		GLES10.glDepthMask(true);
		GLES10.glFrontFace(GLES10.GL_CW);
		GLES10.glCullFace(GLES10.GL_BACK);
		GLES10.glEnable(GLES10.GL_TEXTURE_2D);
		GLES10.glEnable(GLES10.GL_BLEND);
		GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
		GLES10.glDisable(GLES10.GL_CULL_FACE);
		GLES10.glEnable(GLES10.GL_ALPHA_TEST);
		GLES10.glAlphaFunc(GLES10.GL_GREATER, 0.0F);
		
		TextureManager.loadTexture(this.context, R.drawable.terrain, 1);
		TextureManager.loadTexture(this.context, R.drawable.bg_top, 2);
		TextureManager.loadTexture(this.context, R.drawable.bg_main, 3);
		TextureManager.loadTexture(this.context, R.drawable.logo, 4);
		TextureManager.loadTexture(this.context, R.drawable.button, 5);
		TextureManager.loadTexture(this.context, R.drawable.button_highlighted, 6);
		TextureManager.loadTexture(this.context, R.drawable.button_big, 7);
		TextureManager.loadTexture(this.context, R.drawable.button_big_highlighted, 8);
		TextureManager.loadTexture(this.context, R.drawable.donate, 9);
		TextureManager.loadTexture(this.context, R.drawable.server_list_entry, 10);
		TextureManager.loadTexture(this.context, R.drawable.map_background, 11);
		TextureManager.loadTexture(this.context, R.drawable.terrain_map, 12);
		TextureManager.loadTexture(this.context, R.drawable.login_information_field, 13);
		TextureManager.loadTexture(this.context, R.drawable.button_login, 14);
		TextureManager.loadTexture(this.context, R.drawable.button_change, 15);
		TextureManager.loadTexture(this.context, R.drawable.button_login_highlighted, 16);
		TextureManager.loadTexture(this.context, R.drawable.button_change_highlighted, 17);
		TextureManager.loadTexture(this.context, R.drawable.loading_background_a, 18);
		TextureManager.loadTexture(this.context, R.drawable.loading_background_b, 19);
		TextureManager.loadTexture(this.context, R.drawable.info_background, 21);
		TextureManager.loadTexture(this.context, R.drawable.button_down, 22);
		TextureManager.loadTexture(this.context, R.drawable.button_down_highlighted, 23);
		TextureManager.loadTexture(this.context, R.drawable.button_up, 24);
		TextureManager.loadTexture(this.context, R.drawable.button_up_highlighted, 25);
		TextureManager.loadTexture(this.context, R.drawable.button_scrollbar, 26);
		TextureManager.loadTexture(this.context, R.drawable.button_chat, 27);
		TextureManager.loadTexture(this.context, R.drawable.button_chat_highlighted, 28);
		TextureManager.loadTexture(this.context, R.drawable.button_jump, 29);
		TextureManager.loadTexture(this.context, R.drawable.button_jump_highlighted, 30);
		TextureManager.loadTexture(this.context, R.drawable.skin, 31);
		TextureManager.loadTexture(this.context, R.drawable.button_list, 32);
		TextureManager.loadTexture(this.context, R.drawable.button_list_highlighted, 33);
		TextureManager.loadTexture(this.context, R.drawable.button_blocks, 34);
		TextureManager.loadTexture(this.context, R.drawable.button_blocks_highlighted, 35);
		TextureManager.loadTexture(this.context, R.drawable.touch_controls, 36);
		TextureManager.loadTexture(this.context, R.drawable.touch_controls_joystick, 37);
	}
	
}

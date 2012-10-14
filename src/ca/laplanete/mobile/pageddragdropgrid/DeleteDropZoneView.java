package ca.laplanete.mobile.pageddragdropgrid;


import ca.laplanete.mobile.example.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class DeleteDropZoneView extends View {

	private Paint mTextPaintStraight;
	private Paint mTextPaintRed;
	private boolean straight = true;


	public DeleteDropZoneView(Context context) {
		super(context);
		
		mTextPaintStraight = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaintStraight.setColor(Color.WHITE);
		mTextPaintStraight.setStyle(Style.FILL); 
		mTextPaintStraight.setTextSize(20);
		
		mTextPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaintRed.setColor(Color.RED);
		mTextPaintRed.setStyle(Style.FILL); 	    
		mTextPaintRed.setTextSize(20);
		
		setBackgroundColor(Color.DKGRAY);
		getBackground().setAlpha(70);	   
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		  super.onDraw(canvas);
		 if (straight) {
			 canvas.drawText(getResources().getString(R.string.removeItem), 10, 35, mTextPaintStraight);
		 } else {
			 canvas.drawText(getResources().getString(R.string.removeItem), 10, 35, mTextPaintRed);
		 }
	}

	public void highlight() {
		straight = false;
		invalidate();
	}

	public void smother() {
		straight = true;
		invalidate();
	}
	

}

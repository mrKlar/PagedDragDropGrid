package ca.laplanete.mobile.pageddragdropgrid;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import ca.laplanete.mobile.example.R;

public class DeleteDropZoneView extends View {

	private Paint mTextPaintStraight;
	private Paint mTextPaintRed;
	private boolean straight = true;

	public DeleteDropZoneView(Context context) {
		super(context);

		mTextPaintStraight = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaintStraight.setColor(Color.WHITE);
		mTextPaintStraight.setStyle(Style.FILL); 		
		mTextPaintStraight.setTypeface(Typeface.DEFAULT_BOLD);
		
		mTextPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaintRed.setColor(Color.RED);
		mTextPaintRed.setStyle(Style.FILL); 	    
		mTextPaintRed.setTypeface(Typeface.DEFAULT_BOLD);
		
		setBackgroundColor(Color.BLACK);
		getBackground().setAlpha(100);	   
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
	
		super.onDraw(canvas);
		String removeString = getResources().getString(R.string.removeItem);
		
		int stringLength = (int)mTextPaintStraight.measureText(removeString) / 2;
		Rect bounds = new Rect();
		mTextPaintStraight.getTextBounds("X",0,1,bounds);
		
		int proportion = 3 * getMeasuredHeight() / 4;
		if (straight) {	
			mTextPaintStraight.setTextSize(proportion);
			canvas.drawText(removeString, 10, getMeasuredHeight() - ((getMeasuredHeight() - bounds.height()) / 2) , mTextPaintStraight);
		 } else {
			mTextPaintRed.setTextSize(proportion);
			canvas.drawText(removeString, 10, getMeasuredHeight() - ((getMeasuredHeight() - bounds.height()) / 2), mTextPaintRed);
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

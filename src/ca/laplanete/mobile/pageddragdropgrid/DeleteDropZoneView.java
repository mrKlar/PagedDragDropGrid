package ca.laplanete.mobile.pageddragdropgrid;


import ca.laplanete.mobile.example.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class DeleteDropZoneView extends View {

	private Paint mTextPaint;


	public DeleteDropZoneView(Context context) {
		super(context);
		
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setStyle(Style.FILL); 

	        
		mTextPaint.setTextSize(20);
		
		setBackgroundColor(Color.DKGRAY);
		getBackground().setAlpha(70);	   
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		   super.onDraw(canvas);

		   canvas.drawText(getResources().getString(R.string.removeItem), 10, 35, mTextPaint);
	}
	

}

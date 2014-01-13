/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.dmfs.tasks.groupings;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.tasks.R;
import org.dmfs.tasks.groupings.cursorloaders.TimeRangeCursorFactory;
import org.dmfs.tasks.groupings.cursorloaders.TimeRangeCursorLoaderFactory;
import org.dmfs.tasks.groupings.cursorloaders.TimeRangeShortCursorFactory;
import org.dmfs.tasks.utils.ExpandableChildDescriptor;
import org.dmfs.tasks.utils.ExpandableGroupDescriptor;
import org.dmfs.tasks.utils.ExpandableGroupDescriptorAdapter;
import org.dmfs.tasks.utils.ViewDescriptor;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Build;
import android.text.format.Time;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Definition of the by-start date grouping.
 * 
 * 
 * 
 * @author Tobias Reinsch <tobias@dmfs.org>
 */
public interface ByStartDate
{
	/**
	 * A {@link ViewDescriptor} that knows how to present the tasks in the task list.
	 */
	public final ViewDescriptor TASK_VIEW_DESCRIPTOR = new ViewDescriptor()
	{
		/**
		 * We use this to get the current time.
		 */
		private Time mNow;

		/**
		 * The formatter we use for due dates other than today.
		 */
		private final DateFormat mDateFormatter = DateFormat.getDateInstance(SimpleDateFormat.MEDIUM);

		/**
		 * The formatter we use for tasks that are due today.
		 */
		private final DateFormat mTimeFormatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);


		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void populateView(View view, Cursor cursor, BaseExpandableListAdapter adapter, int flags)
		{
			TextView title = (TextView) view.findViewById(android.R.id.title);
			boolean isClosed = cursor.getInt(13) > 0;

			if (android.os.Build.VERSION.SDK_INT >= 14)
			{
				view.setTranslationX(0);
				view.setAlpha(1);
			}
			else
			{
				int paddingTop = view.getPaddingTop();
				int paddingBottom = view.getPaddingBottom();
				view.setPadding(0, paddingTop, 0, paddingBottom);
			}

			if (title != null)
			{
				String text = cursor.getString(5);
				title.setText(text);
				if (isClosed)
				{
					title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				}
				else
				{
					title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
				}
			}
			TextView startDateField = (TextView) view.findViewById(R.id.task_start_date);
			if (startDateField != null)
			{
				Time startDate = Common.START_DATE_ADAPTER.get(cursor);

				if (startDate != null)
				{
					if (mNow == null)
					{
						mNow = new Time();
					}
					mNow.clear(TimeZone.getDefault().getID());
					mNow.setToNow();

					startDateField.setVisibility(View.VISIBLE);
					startDateField.setText(makeDateString(startDate));

					ImageView icon = (ImageView) view.findViewById(R.id.task_start_image);
					if (icon != null)
					{
						icon.setVisibility(View.VISIBLE);
					}

					// highlight overdue dates & times
					if (startDate.before(mNow) && !isClosed)
					{
						startDateField.setTextAppearance(view.getContext(), R.style.task_list_overdue_text);
					}
					else
					{
						startDateField.setTextAppearance(view.getContext(), R.style.task_list_due_text);
					}
				}
				else
				{
					startDateField.setText("");
				}
			}

			TextView dueDateField = (TextView) view.findViewById(R.id.task_due_date);
			if (dueDateField != null)
			{
				Time dueTime = Common.DUE_ADAPTER.get(cursor);

				if (dueTime != null)
				{
					if (mNow == null)
					{
						mNow = new Time();
					}
					mNow.clear(TimeZone.getDefault().getID());
					mNow.setToNow();

					dueDateField.setText(makeDateString(dueTime));
					ImageView icon = (ImageView) view.findViewById(R.id.task_due_image);
					if (icon != null)
					{
						icon.setVisibility(View.VISIBLE);
					}

					// highlight overdue dates & times
					if (dueTime.before(mNow) && !isClosed)
					{
						dueDateField.setTextAppearance(view.getContext(), R.style.task_list_overdue_text);
					}
					else
					{
						dueDateField.setTextAppearance(view.getContext(), R.style.task_list_due_text);
					}
				}
				else
				{
					dueDateField.setText("");
				}
			}

			View colorbar = view.findViewById(R.id.colorbar);
			if (colorbar != null)
			{
				colorbar.setBackgroundColor(cursor.getInt(6));
			}

			View divider = view.findViewById(R.id.divider);
			if (divider != null)
			{
				divider.setVisibility((flags & FLAG_IS_LAST_CHILD) != 0 ? View.GONE : View.VISIBLE);
			}

			// display priority
			int priority = cursor.getInt(cursor.getColumnIndex(Instances.PRIORITY));
			View priorityView = view.findViewById(R.id.task_priority_view_medium);
			priorityView.setBackgroundResource(android.R.color.transparent);
			priorityView.setVisibility(View.VISIBLE);

			if (priority > 0 && priority < 5)
			{
				priorityView.setBackgroundResource(R.color.priority_red);
			}
			if (priority == 5)
			{
				priorityView.setBackgroundResource(R.color.priority_yellow);
			}
			if (priority > 5 && priority <= 9)
			{
				priorityView.setBackgroundResource(R.color.priority_green);
			}
		}


		@Override
		public int getView()
		{
			return R.layout.task_list_element;
		}


		/**
		 * Get the date to show. It returns just a time for dates that are today and a date otherwise.
		 * 
		 * @param date
		 *            The date to format.
		 * @return A String with the formatted date.
		 */
		private String makeDateString(Time date)
		{
			if (!date.allDay)
			{
				date.switchTimezone(TimeZone.getDefault().getID());
			}

			if (date.year == mNow.year && date.yearDay == mNow.yearDay)
			{
				return mTimeFormatter.format(new Date(date.toMillis(false)));
			}
			else
			{
				return mDateFormatter.format(new Date(date.toMillis(false)));
			}
		}
	};

	/**
	 * A {@link ViewDescriptor} that knows how to present start date groups.
	 */
	public final ViewDescriptor GROUP_VIEW_DESCRIPTOR = new ViewDescriptor()
	{
		// DateFormatSymbols.getInstance() not used because it is not available before API level 9
		private final String[] mMonthNames = new DateFormatSymbols().getMonths();


		@Override
		public void populateView(View view, Cursor cursor, BaseExpandableListAdapter adapter, int flags)
		{
			int position = cursor.getPosition();

			// set list title
			TextView title = (TextView) view.findViewById(android.R.id.title);
			if (title != null)
			{
				title.setText(getTitle(cursor, view.getContext()));
			}

			// set list elements
			TextView text2 = (TextView) view.findViewById(android.R.id.text2);
			int childrenCount = adapter.getChildrenCount(position);
			if (text2 != null && ((ExpandableGroupDescriptorAdapter) adapter).childCursorLoaded(position))
			{
				Resources res = view.getContext().getResources();
				text2.setText(res.getQuantityString(R.plurals.number_of_tasks, childrenCount, childrenCount));

			}

			// show/hide divider
			View divider = view.findViewById(R.id.divider);
			if (divider != null)
			{
				divider.setVisibility((flags & FLAG_IS_EXPANDED) != 0 && childrenCount > 0 ? View.VISIBLE : View.GONE);
			}

			View colorbar = view.findViewById(R.id.colorbar1);
			if (colorbar != null)
			{
				colorbar.setVisibility(View.GONE);
			}
		}


		@Override
		public int getView()
		{
			return R.layout.task_list_group_single_line;
		}


		/**
		 * Return the title of a date group.
		 * 
		 * @param cursor
		 *            A {@link Cursor} pointing to the current group.
		 * @return A {@link String} with the group name.
		 */
		private String getTitle(Cursor cursor, Context context)
		{
			int type = cursor.getInt(cursor.getColumnIndex(TimeRangeCursorFactory.RANGE_TYPE));
			if (type == 0)
			{
				return context.getString(R.string.task_group_no_due);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_OF_TODAY) == TimeRangeCursorFactory.TYPE_END_OF_TODAY)
			{
				return context.getString(R.string.task_group_due_today);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_OF_YESTERDAY) == TimeRangeCursorFactory.TYPE_END_OF_YESTERDAY)
			{
				return context.getString(R.string.task_group_overdue);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_OF_TOMORROW) == TimeRangeCursorFactory.TYPE_END_OF_TOMORROW)
			{
				return context.getString(R.string.task_group_due_tomorrow);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_IN_7_DAYS) == TimeRangeCursorFactory.TYPE_END_IN_7_DAYS)
			{
				return context.getString(R.string.task_group_due_within_7_days);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_OF_A_MONTH) != 0)
			{
				return context.getString(R.string.task_group_due_in_month,
					mMonthNames[cursor.getInt(cursor.getColumnIndex(TimeRangeCursorFactory.RANGE_MONTH))]);
			}
			if ((type & TimeRangeCursorFactory.TYPE_END_OF_A_YEAR) != 0)
			{
				return context.getString(R.string.task_group_due_in_year, cursor.getInt(cursor.getColumnIndex(TimeRangeCursorFactory.RANGE_YEAR)));
			}
			if ((type & TimeRangeCursorFactory.TYPE_NO_END) != 0)
			{
				return context.getString(R.string.task_group_due_in_future);
			}
			return "";
		}

	};

	/**
	 * A descriptor that knows how to load elements in a start date group.
	 */
	public final static ExpandableChildDescriptor START_DATE_DESCRIPTOR = new ExpandableChildDescriptor(Instances.CONTENT_URI, Common.INSTANCE_PROJECTION,
		Instances.VISIBLE + "=1 and (((" + Instances.INSTANCE_START + ">=?) and (" + Instances.INSTANCE_START + "<?)) or ((" + Instances.INSTANCE_START
			+ ">=? or " + Instances.INSTANCE_START + " is ?) and ? is null))", Instances.DEFAULT_SORT_ORDER, 0, 1, 0, 1, 1)
		.setViewDescriptor(TASK_VIEW_DESCRIPTOR);

	/**
	 * A descriptor for the "grouped by due date" view.
	 */
	public final static ExpandableGroupDescriptor GROUP_DESCRIPTOR = new ExpandableGroupDescriptor(new TimeRangeCursorLoaderFactory(
		TimeRangeShortCursorFactory.DEFAULT_PROJECTION), START_DATE_DESCRIPTOR).setViewDescriptor(GROUP_VIEW_DESCRIPTOR);

}
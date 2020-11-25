/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.apromore.calendar.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apromore.calendar.exception.CalendarAlreadyExistsException;
import org.apromore.calendar.exception.CalendarNotExistsException;
import org.apromore.calendar.model.CalendarModel;
import org.apromore.calendar.model.HolidayModel;
import org.apromore.calendar.util.CalendarUtil;
import org.apromore.commons.mapper.CustomMapper;
import org.apromore.dao.CustomCalendarInfoRepository;
import org.apromore.dao.CustomCalendarRepository;
import org.apromore.dao.HolidayRepository;
import org.apromore.dao.model.CustomCalendar;
import org.apromore.dao.model.HOLIDAYTYPE;
import org.apromore.dao.model.Holiday;
import org.apromore.dao.model.WorkDay;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import lombok.Data;

@Data
public class CustomCalendarService implements CalendarService {

	@Autowired
	public CustomCalendarRepository calendarRepo;

	@Autowired
	public CustomCalendarInfoRepository calendarInfoRepo;

	@Autowired
	public HolidayRepository holidayRepository;

	@Autowired
	private CustomMapper modelMapper;

	@Override
	public CalendarModel createGenericCalendar(String description, boolean weekendsOff, String zoneId)
			throws CalendarAlreadyExistsException {

		OffsetTime startTime = OffsetTime.of(LocalTime.MIN, ZoneId.of(zoneId).getRules().getOffset(Instant.now()));

		OffsetTime endTime = OffsetTime.of(LocalTime.MAX, ZoneId.of(zoneId).getRules().getOffset(Instant.now()));

		CustomCalendar customCalender = createCalendar(description, weekendsOff, startTime, endTime);
		CalendarModel calenderModel = modelMapper.getMapper().map(customCalender, CalendarModel.class);
		return calenderModel;
	}

	@Override
	public CalendarModel createBusinessCalendar(String description, boolean weekendsOff, String zoneId)
			throws CalendarAlreadyExistsException {

		OffsetTime startTime = OffsetTime.of(LocalTime.of(9, 0), ZoneId.of(zoneId).getRules().getOffset(Instant.now()));
		OffsetTime endTime = OffsetTime.of(LocalTime.of(17, 0), ZoneId.of(zoneId).getRules().getOffset(Instant.now()));

		CustomCalendar customCalender = createCalendar(description, weekendsOff, startTime, endTime);
		CalendarModel calenderModel = modelMapper.getMapper().map(customCalender, CalendarModel.class);

		return calenderModel;

	}

	@Override
	public CalendarModel getCalender(Long id) {

		return modelMapper.getMapper().map(calendarRepo.findById(id), CalendarModel.class);

	}

	@Override
	public List<CalendarModel> getCalendars() {

		List<CalendarModel> calendarModels = modelMapper.getMapper().map(calendarInfoRepo.findAll(),
				new TypeToken<List<CalendarModel>>() {
				}.getType());

		return calendarModels;

	}

	private CustomCalendar createCalendar(String description, boolean weekendsOff, OffsetTime start, OffsetTime end)
			throws CalendarAlreadyExistsException {

		validateCalenderExists(calendarRepo.findByName(description));

		final CustomCalendar calendar = new CustomCalendar(description);
		for (WorkDay workDay : getWorkDays(start, end, weekendsOff)) {
			calendar.addWorkDay(workDay);
		}
		CustomCalendar newcalender = calendarRepo.saveAndFlush(calendar);
		return newcalender;

	}

	private List<WorkDay> getWorkDays(OffsetTime start, OffsetTime end, boolean weekendOff) {

		Predicate<DayOfWeek> isWeekendOff = CalendarUtil.getWeekendOffPRedicate(weekendOff);
		List<WorkDay> workDaysList = new ArrayList<WorkDay>();

		for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
			workDaysList.add(new WorkDay(dayOfWeek, start, end, !isWeekendOff.test(dayOfWeek)));
		}
		return workDaysList;

	}

	private void validateCalenderExists(CustomCalendar calendar) throws CalendarAlreadyExistsException {

		if (calendar != null) {
			throw new CalendarAlreadyExistsException("Calendar already exists");
		}

	}

	public void updateHoliday(Long id, List<HolidayModel> holidayModels) throws CalendarNotExistsException {
		CustomCalendar calendar = calendarRepo.findById(id);

		if (calendar == null) {
			throw new CalendarNotExistsException("calender does not exist");
		}

		List<Holiday> holidays=new ArrayList<Holiday>();
		for(HolidayModel h: holidayModels)
		{
			holidays.add(new Holiday(h.getName(),
					h.getDescription(),
					h.getHolidayDate(),
					HOLIDAYTYPE.valueOf(h.getHolidayType())));
		}

		calendar.getHolidays().clear();
		for (Holiday holiday : holidays) {
			calendar.addHoliday(holiday);
		}
		calendarRepo.saveAndFlush(calendar);

	}

	public void removeHoliday(Long id, List<Long> holidayIds) {

		List<Holiday> holidays = new ArrayList<Holiday>();
		// Bad way to delete, but we need to upgrade jpa of springs to make use of
		// better ways of delete
		for (Long idLong : holidayIds) {
			holidayRepository.delete(idLong);

		}

	}

	@Override
	public void deleteCalender(Long calendarId) {
		calendarRepo.delete(calendarId);

	}

}

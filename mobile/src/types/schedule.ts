export type Weekday = 1 | 2 | 3 | 4 | 5;

export type ScheduleSlot = {
  dia: Weekday;
  horaInicio: string;
};

export type Schedule = {
  franjas: ScheduleSlot[];
};

import { colors } from "./colors";
import { bangers } from "./bangers";

export const typography = {
  title: {
    color: colors.white,
    ...bangers(34),
    letterSpacing: 1,
  },
  heading: {
    color: colors.text,
    fontFamily: "ZenMaruBold",
    fontSize: 20,
  },
  body: {
    color: colors.text,
    fontFamily: "ZenMaru",
    fontSize: 16,
  },
} as const;

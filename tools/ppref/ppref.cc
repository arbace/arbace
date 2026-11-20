/* ppref: the token checks' reference (doc/cpp/CPP-SURVEY.md, "As built: pp-ref"; CP413 (a), CP414 (a), the user's of 2026-11-20).

   Arbace's own harness (EPL-1.0, as the rest of Arbace's own code; no line of GCC in it) around GCC 15.2.0's libcpp, unmodified
   from the pinned gcc-15.2.0 tarball (test/cpp/corpora.edn's :gcc-15.2.0). The two are put together only in .tmp by
   bin/repr-cpp ppref build (LICENSE.md: libcpp is GPL-3.0-or-later; its build is never committed, packed or pushed). It does what
   the preprocessing part of cc1plus (and cc1) does around libcpp for `g++ -E -P` (gcc -E -P), from the target's description
   given by the check (the predefined macros, the __has_ answers, the search list): the options, the builtins and the target's
   macros in <built-in>, -include, the diagnostics' severities (c-family's), and prints the stream's tokens; or lexes the main file
   raw, as clang's -dump-raw-tokens does, and prints each token's position and text.

   ppref pp|lex [HARNESS OPTION...] [--] [COMPILER OPTION...] FILE
     pp    the preprocessed stream: one record a token (identifiers `i`, other tokens `t`; a user-defined literal as the literal
           and its suffix, an #embed's bytes as numbers and commas), pragmas left out; the exit status 1 when an error was
           reported (c-family's: a pedwarn under -pedantic-errors, a warning under -Werror), 4 on libcpp's internal error
     lex   the main file's raw tokens (no directive run, no macro expanded, no comment), one record a token: LINE:COL<tab>TEXT,
           the token placed at the line splices that touch it from the left and its text without splices (the check's raw-lines)
   A record is `KLEN TEXT\n`: the kind letter, the text's length in bytes, a space, the text.
   Harness options:
     --lang c|c++       the language (default c++)
     --predef FILE      the target's predefined macros (#define and #undef lines), defined in <built-in> after libcpp's own
     --has FILE         the target's __has_ answers: OP<tab>NAME<tab>VALUE lines (the check's own table, cpp/target/host/has.tsv's
                        first and third columns)
     --sys DIR          a directory of the standard search list, in order (system headers)
     --sysroot DIR      the directory the standard directories are read under: a --sys DIR is DIR's absolute name under it, and
                        __FILE__ names such a header by its name without the prefix (as g++ names it)
     --epoch N          SOURCE_DATE_EPOCH (__DATE__, __TIME__)
     --list FILE        many files in one process: FILE's lines `OUT<tab>ARG<tab>ARG...` (the compiler options and the file, tab
                        separated), each file's records into OUT and its status into OUT.status
   Compiler options modelled: -std=, -ansi, -x c|c++, -I, -iquote, -isystem, -idirafter, -include, -nostdinc, -nostdinc++,
   -trigraphs, -no-trigraphs (none), -pedantic, -Wpedantic, -pedantic-errors, -w, -Werror, -Werror=, -Wno-error=, -W[no-]NAME
   (the preprocessor's warnings), -f[no-]extended-identifiers, -f[no-]dollars-in-identifiers, -f[no-]ext-numeric-literals,
   -f[no-]operator-names, -funsigned-char, -fsigned-char, -fwide-exec-charset=, -fexec-charset=, -finput-charset=; -D and -U
   are the target's (the check gives their effect in --predef); every other option is ignored. */

#include "config.h"
#include "system.h"
#include "cpplib.h"
#include "internal.h"

/* gcc/input.h's: the location of the builtins' definitions */
#define BUILTINS_LOCATION ((location_t) 1)

/* the diagnostics' stream (a list's file's OUT.err) */
static FILE *errout;

/* ------------------------------------------------------------------------------------------------------------------------ */
/* libcpp's client: what GCC's diagnostic and input modules give it */

void
fancy_abort (const char *file, int line, const char *function)
{
  fprintf (errout ? errout : stderr, "ppref: internal error at %s:%d (%s)\n", file, line, function);
  fflush (stdout);
  exit (4);
}

expanded_location
linemap_client_expand_location_to_spelling_point (const line_maps *set, location_t loc, enum location_aspect)
{
  const line_map_ordinary *map;
  loc = linemap_resolve_location (set, loc, LRK_SPELLING_LOCATION, &map);
  return linemap_expand_location (set, map, loc);
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* The harness's state (one file at a time) */

struct has_row
{
  char *op;
  char *name;
  long value;
};

struct dir_row
{
  char *name;
  int sysp;
};

static int lang_cxx = 1;
static const char *predef_file;
static const char *has_file;
static const char *sysroot;
static size_t sysroot_len;
static long long epoch = -1;

static struct has_row *has_rows;
static int n_has;
static char *predef_text;

static struct dir_row *std_dirs;
static int n_std_dirs;

/* per file */
static line_maps *lt;
static cpp_reader *reader;
static int errors;
static int fatal;
static int quiet;  /* lex: no diagnostic counts */
static int pedantic_errors;
static int werror;
static int no_warnings;
static int wsystem_headers;
static unsigned char werror_on[64];
static unsigned char werror_off[64];
static unsigned char warning_off[64];
static FILE *out;

static void *
xalloc0 (size_t n)
{
  void *p = xmalloc (n ? n : 1);
  memset (p, 0, n ? n : 1);
  return p;
}

static char *
read_all (const char *path, size_t *len)
{
  FILE *f = fopen (path, "rb");
  if (!f)
    return NULL;
  size_t cap = 65536, n = 0;
  char *buf = (char *) xmalloc (cap + 1);
  for (;;)
    {
      if (n == cap)
	{
	  cap *= 2;
	  buf = (char *) xrealloc (buf, cap + 1);
	}
      size_t k = fread (buf + n, 1, cap - n, f);
      if (k == 0)
	break;
      n += k;
    }
  fclose (f);
  buf[n] = '\0';
  if (len)
    *len = n;
  return buf;
}

static void
load_has (const char *path)
{
  size_t len;
  char *text = read_all (path, &len);
  if (!text)
    {
      fprintf (stderr, "ppref: cannot read %s\n", path);
      exit (2);
    }
  int cap = 256;
  has_rows = (struct has_row *) xmalloc (cap * sizeof (struct has_row));
  char *p = text;
  while (*p)
    {
      char *nl = strchr (p, '\n');
      if (nl)
	*nl = '\0';
      char *t1 = strchr (p, '\t');
      char *t2 = t1 ? strchr (t1 + 1, '\t') : NULL;
      if (t1 && t2)
	{
	  *t1 = '\0';
	  *t2 = '\0';
	  if (n_has == cap)
	    {
	      cap *= 2;
	      has_rows = (struct has_row *) xrealloc (has_rows, cap * sizeof (struct has_row));
	    }
	  has_rows[n_has].op = p;
	  has_rows[n_has].name = t1 + 1;
	  has_rows[n_has].value = strtol (t2 + 1, NULL, 10);
	  n_has++;
	}
      if (!nl)
	break;
      p = nl + 1;
    }
}

static long
has_value_or (const char *op, const char *name, long otherwise)
{
  for (int i = 0; i < n_has; i++)
    if (strcmp (has_rows[i].op, op) == 0 && strcmp (has_rows[i].name, name) == 0)
      return has_rows[i].value;
  return otherwise;
}

static long
has_value (const char *op, const char *name)
{
  return has_value_or (op, name, 0);
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* Diagnostics: c-family's c_cpp_diagnostic's severities, counted */

struct warning_name
{
  const char *name;
  int reason;
};

static const struct warning_name warning_names[] = {
  {"deprecated", CPP_W_DEPRECATED}, {"comment", CPP_W_COMMENTS}, {"comments", CPP_W_COMMENTS},
  {"missing-include-dirs", CPP_W_MISSING_INCLUDE_DIRS}, {"trigraphs", CPP_W_TRIGRAPHS}, {"multichar", CPP_W_MULTICHAR},
  {"traditional", CPP_W_TRADITIONAL}, {"long-long", CPP_W_LONG_LONG}, {"endif-labels", CPP_W_ENDIF_LABELS},
  {"sign-change", CPP_W_NUM_SIGN_CHANGE}, {"variadic-macros", CPP_W_VARIADIC_MACROS},
  {"builtin-macro-redefined", CPP_W_BUILTIN_MACRO_REDEFINED}, {"dollars", CPP_W_DOLLARS}, {"undef", CPP_W_UNDEF},
  {"unused-macros", CPP_W_UNUSED_MACROS}, {"c++-compat", CPP_W_CXX_OPERATOR_NAMES}, {"normalized", CPP_W_NORMALIZE},
  {"invalid-pch", CPP_W_INVALID_PCH}, {"cpp", CPP_W_WARNING_DIRECTIVE}, {"literal-suffix", CPP_W_LITERAL_SUFFIX},
  {"size-t-literals", CPP_W_SIZE_T_LITERALS}, {"date-time", CPP_W_DATE_TIME}, {"pedantic", CPP_W_PEDANTIC},
  {"c90-c99-compat", CPP_W_C90_C99_COMPAT}, {"c11-c23-compat", CPP_W_C11_C23_COMPAT},
  {"c23-c2y-compat", CPP_W_C23_C2Y_COMPAT}, {"c++11-compat", CPP_W_CXX11_COMPAT}, {"c++0x-compat", CPP_W_CXX11_COMPAT},
  {"c++20-compat", CPP_W_CXX20_COMPAT}, {"c++2a-compat", CPP_W_CXX20_COMPAT},
  {"c++14-extensions", CPP_W_CXX14_EXTENSIONS}, {"c++17-extensions", CPP_W_CXX17_EXTENSIONS},
  {"c++20-extensions", CPP_W_CXX20_EXTENSIONS}, {"c++23-extensions", CPP_W_CXX23_EXTENSIONS},
  {"c++26-extensions", CPP_W_CXX26_EXTENSIONS}, {"expansion-to-defined", CPP_W_EXPANSION_TO_DEFINED},
  {"bidi-chars", CPP_W_BIDIRECTIONAL}, {"invalid-utf8", CPP_W_INVALID_UTF8}, {"unicode", CPP_W_UNICODE},
  {"header-guard", CPP_W_HEADER_GUARD}, {"pragma-once-outside-header", CPP_W_PRAGMA_ONCE_OUTSIDE_HEADER},
  {"leading-whitespace", CPP_W_LEADING_WHITESPACE}, {"trailing-whitespace", CPP_W_TRAILING_WHITESPACE},
  {NULL, 0}};

static int
warning_reason (const char *name)
{
  for (int i = 0; warning_names[i].name; i++)
    if (strcmp (warning_names[i].name, name) == 0)
      return warning_names[i].reason;
  return -1;
}

static bool
diagnostic (cpp_reader *, enum cpp_diagnostic_level level, enum cpp_warning_reason reason, rich_location *richloc,
	    const char *msg, va_list *ap)
{
  location_t loc = richloc->get_loc ();
  int sys = loc >= RESERVED_LOCATION_COUNT && linemap_location_in_system_header_p (lt, loc) && !wsystem_headers;
  int r = (int) reason;
  int kind = 0;  /* 0 none, 1 warning, 2 error, 3 fatal, 4 internal */
  switch (level)
    {
    case CPP_DL_WARNING_SYSHDR:
    case CPP_DL_WARNING:
      if (level == CPP_DL_WARNING && sys)
	kind = 0;
      else if (r > 0 && r < 64 && warning_off[r])
	kind = 0;
      else if (r > 0 && r < 64 && werror_on[r])
	kind = 2;
      else if (no_warnings)
	kind = 0;
      else if (werror && !(r > 0 && r < 64 && werror_off[r]))
	kind = 2;
      else
	kind = 1;
      break;
    case CPP_DL_PEDWARN:
      if (sys)
	kind = 0;
      else if (r > 0 && r < 64 && warning_off[r])
	kind = 0;
      else if (pedantic_errors || (r > 0 && r < 64 && werror_on[r]))
	kind = 2;
      else if (no_warnings)
	kind = 0;
      else if (werror && !(r > 0 && r < 64 && werror_off[r]))
	kind = 2;
      else
	kind = 1;
      break;
    case CPP_DL_ERROR:
      kind = 2;
      break;
    case CPP_DL_FATAL:
      kind = 3;
      break;
    case CPP_DL_ICE:
      kind = 4;
      break;
    default:
      kind = 0;
    }
  if (kind == 0 || quiet)
    return false;
  expanded_location x = linemap_client_expand_location_to_spelling_point (lt, loc, LOCATION_ASPECT_CARET);
  fprintf (errout, "%s:%d:%d: %s: ", x.file ? x.file : "<built-in>", x.line, x.column,
	   kind == 1 ? "warning" : kind == 3 ? "fatal error" : kind == 4 ? "internal error" : "error");
  (void) ap;
  fputs (msg, errout);  /* the message's template: GCC's %< %> are no printf's */
  fputc ('\n', errout);
  if (kind >= 2)
    errors++;
  if (kind == 4)
    {
      fflush (out);
      exit (4);
    }
  if (kind == 3)
    fatal = 1;
  return true;
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* The __has_ operators: c-family's parse of their operand, the target's answers */

static const cpp_token *
next_token (cpp_reader *pfile)
{
  for (;;)
    {
      const cpp_token *t = cpp_peek_token (pfile, 0);
      if (t->type == CPP_EOF)
	return t;
      t = cpp_get_token (pfile);
      if (t->type != CPP_PADDING)
	return t;
    }
}

static int
has_attribute (cpp_reader *pfile, bool std_syntax)
{
  /* libcpp gives __has_cpp_attribute and __has_attribute the same hook (BT_HAS_ATTRIBUTE, std_syntax false: init.cc's builtin
     table), so g++ answers both alike; the table holds the spellings the checks asked, either one is the answer. C's
     __has_c_attribute is the standard syntax's. */
  const char *op = std_syntax ? "__has_c_attribute" : "__has_attribute";
  const cpp_token *t = next_token (pfile);
  if (t->type != CPP_OPEN_PAREN)
    {
      cpp_error (pfile, CPP_DL_ERROR, "missing '(' after \"__has_attribute\"");
      return 0;
    }
  t = next_token (pfile);
  if (t->type != CPP_NAME)
    {
      cpp_error (pfile, CPP_DL_ERROR, "macro \"__has_attribute\" requires an identifier");
      return 0;
    }
  char *name = xstrdup ((const char *) cpp_token_as_text (pfile, t));
  long result = 0;
  int known = 1;
  int idx = 0;
  const cpp_token *nxt;
  do
    nxt = cpp_peek_token (pfile, idx++);
  while (nxt->type == CPP_PADDING);
  int have_scope = 0;
  if (!lang_cxx && nxt->type == CPP_COLON && (nxt->flags & COLON_SCOPE) != 0)
    {
      const cpp_token *prev = nxt;
      do
	nxt = cpp_peek_token (pfile, idx++);
      while (nxt->type == CPP_PADDING);
      if (nxt->type == CPP_COLON)
	{
	  have_scope = 1;
	  next_token (pfile);
	}
      else
	nxt = prev;
    }
  if (nxt->type == CPP_SCOPE || have_scope)
    {
      next_token (pfile);
      const cpp_token *id = next_token (pfile);
      if (id->type == CPP_NAME)
	{
	  const char *s = (const char *) cpp_token_as_text (pfile, id);
	  char *full = (char *) xmalloc (strlen (name) + strlen (s) + 3);
	  sprintf (full, "%s::%s", name, s);
	  free (name);
	  name = full;
	}
      else
	{
	  cpp_error (pfile, CPP_DL_ERROR, "attribute identifier required after scope");
	  known = 0;
	}
    }
  if (known)
    {
      result = has_value_or (op, name, -1);
      if (result == -1 && !std_syntax)
	result = has_value_or (lang_cxx ? "__has_cpp_attribute" : "__has_c_attribute", name, -1);
      if (result == -1)
	result = 0;
    }
  free (name);
  if (next_token (pfile)->type != CPP_CLOSE_PAREN)
    cpp_error (pfile, CPP_DL_ERROR, "missing ')' after \"__has_attribute\"");
  return (int) result;
}

static const char *
availability_operand (cpp_reader *pfile, const char *what)
{
  const cpp_token *t = next_token (pfile);
  if (t->type != CPP_OPEN_PAREN)
    {
      cpp_error (pfile, CPP_DL_ERROR, "missing '(' after \"__has_%s\"", what);
      return NULL;
    }
  const char *name = "";
  t = next_token (pfile);
  if (t->type == CPP_NAME)
    {
      name = (const char *) cpp_token_as_text (pfile, t);
      t = next_token (pfile);
      if (t->type != CPP_CLOSE_PAREN)
	{
	  cpp_error (pfile, CPP_DL_ERROR, "expected ')' after \"%s\"", name);
	  name = "";
	}
    }
  else
    {
      cpp_error (pfile, CPP_DL_ERROR, "macro \"__has_%s\" requires an identifier", what);
      if (t->type == CPP_CLOSE_PAREN)
	return NULL;
    }
  for (unsigned nparen = 1;; t = next_token (pfile))
    {
      if (t->type == CPP_OPEN_PAREN)
	++nparen;
      else if (t->type == CPP_CLOSE_PAREN)
	--nparen;
      else if (t->type == CPP_EOF)
	break;
      if (!nparen)
	break;
    }
  return name;
}

static int
has_builtin (cpp_reader *pfile)
{
  const char *name = availability_operand (pfile, "builtin");
  if (!name || !*name)
    return 0;
  return (int) has_value ("__has_builtin", name);
}

static int
has_feature (cpp_reader *pfile, bool strict)
{
  const char *name = availability_operand (pfile, strict ? "feature" : "extension");
  if (!name || !*name)
    return 0;
  return (int) has_value ("__has_feature", name);
}

static time_t
source_date_epoch (cpp_reader *)
{
  return (time_t) epoch;
}

static const char *
remap_filename (const char *name)
{
  if (sysroot && strncmp (name, sysroot, sysroot_len) == 0 && name[sysroot_len] == '/')
    return name + sysroot_len;
  return name;
}

static void
def_pragma (cpp_reader *, location_t)
{
}

/* the line maps' allocation: GCC rounds to its collector's sizes, malloc needs nothing */
static size_t
round_alloc_size (size_t n)
{
  return n;
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* The options */

struct options
{
  enum c_lang lang;
  int cxx;
  int iso;
  int year;
  int pedantic;
  int trigraphs;
  int ext_identifiers;
  int dollars;
  int ext_numeric;
  int operator_names;
  int unsigned_char;
  int nostdinc;
  int nostdincxx;
  int asm_cpp;
  int warn_comments, warn_undef, warn_unused_macros, warn_long_long, warn_endif_labels, warn_trigraphs;
  int warn_traditional, warn_expansion_to_defined, warn_date_time, warn_multichar, warn_invalid_utf8;
  int warn_deprecated, warn_literal_suffix, warn_variadic, warn_builtin_redefined, warn_normalize, warn_header_guard;
  int warn_cxx11_compat, warn_c90_c99, warn_c11_c23, warn_unicode, warn_bidi, warn_missing_include_dirs;
  const char *input_charset;
  const char *narrow_charset;
  const char *wide_charset;
  struct dir_row *quote, *bracket, *isystem, *after, *embed;
  int n_quote, n_bracket, n_isystem, n_after, n_embed;
  const char **includes;
  int n_includes;
};

static void
push_dir (struct dir_row **v, int *n, const char *name, int sysp)
{
  *v = (struct dir_row *) xrealloc (*v, (*n + 1) * sizeof (struct dir_row));
  (*v)[*n].name = xstrdup (name);
  (*v)[*n].sysp = sysp;
  (*n)++;
}

/* -std=NAME: the language, ISO or GNU, the year (C++: 1998 .. 2026; C: 1989 .. 2025) */
struct std_row
{
  const char *name;
  int cxx;
  int iso;
  int year;
  enum c_lang lang;
};

static const struct std_row stds[] = {
    {"c++98", 1, 1, 1998, CLK_CXX98}, {"c++03", 1, 1, 1998, CLK_CXX98}, {"gnu++98", 1, 0, 1998, CLK_GNUCXX},
    {"gnu++03", 1, 0, 1998, CLK_GNUCXX}, {"c++11", 1, 1, 2011, CLK_CXX11}, {"c++0x", 1, 1, 2011, CLK_CXX11},
    {"gnu++11", 1, 0, 2011, CLK_GNUCXX11}, {"gnu++0x", 1, 0, 2011, CLK_GNUCXX11}, {"c++14", 1, 1, 2014, CLK_CXX14},
    {"c++1y", 1, 1, 2014, CLK_CXX14}, {"gnu++14", 1, 0, 2014, CLK_GNUCXX14}, {"gnu++1y", 1, 0, 2014, CLK_GNUCXX14},
    {"c++17", 1, 1, 2017, CLK_CXX17}, {"c++1z", 1, 1, 2017, CLK_CXX17}, {"gnu++17", 1, 0, 2017, CLK_GNUCXX17},
    {"gnu++1z", 1, 0, 2017, CLK_GNUCXX17}, {"c++20", 1, 1, 2020, CLK_CXX20}, {"c++2a", 1, 1, 2020, CLK_CXX20},
    {"gnu++20", 1, 0, 2020, CLK_GNUCXX20}, {"gnu++2a", 1, 0, 2020, CLK_GNUCXX20}, {"c++23", 1, 1, 2023, CLK_CXX23},
    {"c++2b", 1, 1, 2023, CLK_CXX23}, {"gnu++23", 1, 0, 2023, CLK_GNUCXX23}, {"gnu++2b", 1, 0, 2023, CLK_GNUCXX23},
    {"c++26", 1, 1, 2026, CLK_CXX26}, {"c++2c", 1, 1, 2026, CLK_CXX26}, {"gnu++26", 1, 0, 2026, CLK_GNUCXX26},
    {"gnu++2c", 1, 0, 2026, CLK_GNUCXX26},
    {"c89", 0, 1, 1989, CLK_STDC89}, {"c90", 0, 1, 1989, CLK_STDC89}, {"iso9899:1990", 0, 1, 1989, CLK_STDC89},
    {"iso9899:199409", 0, 1, 1994, CLK_STDC94}, {"gnu89", 0, 0, 1989, CLK_GNUC89}, {"gnu90", 0, 0, 1989, CLK_GNUC89},
    {"c99", 0, 1, 1999, CLK_STDC99}, {"c9x", 0, 1, 1999, CLK_STDC99}, {"iso9899:1999", 0, 1, 1999, CLK_STDC99},
    {"gnu99", 0, 0, 1999, CLK_GNUC99}, {"gnu9x", 0, 0, 1999, CLK_GNUC99}, {"c11", 0, 1, 2011, CLK_STDC11},
    {"c1x", 0, 1, 2011, CLK_STDC11}, {"iso9899:2011", 0, 1, 2011, CLK_STDC11}, {"gnu11", 0, 0, 2011, CLK_GNUC11},
    {"gnu1x", 0, 0, 2011, CLK_GNUC11}, {"c17", 0, 1, 2017, CLK_STDC17}, {"c18", 0, 1, 2017, CLK_STDC17},
    {"iso9899:2017", 0, 1, 2017, CLK_STDC17}, {"iso9899:2018", 0, 1, 2017, CLK_STDC17}, {"gnu17", 0, 0, 2017, CLK_GNUC17},
    {"gnu18", 0, 0, 2017, CLK_GNUC17}, {"c23", 0, 1, 2023, CLK_STDC23}, {"c2x", 0, 1, 2023, CLK_STDC23},
    {"iso9899:2024", 0, 1, 2023, CLK_STDC23}, {"gnu23", 0, 0, 2023, CLK_GNUC23}, {"gnu2x", 0, 0, 2023, CLK_GNUC23},
    {"c2y", 0, 1, 2025, CLK_STDC2Y}, {"gnu2y", 0, 0, 2025, CLK_GNUC2Y},
    {NULL, 0, 0, 0, CLK_GNUCXX17}};

static int
set_std (struct options *o, const char *s)
{
  for (int i = 0; stds[i].name; i++)
    if (strcmp (stds[i].name, s) == 0)
      {
	if (stds[i].cxx != o->cxx)
	  return 0;  /* g++ refuses a C standard and gcc a C++ one: the option is left, as the check's driver gives none */
	o->lang = stds[i].lang;
	o->iso = stds[i].iso;
	o->year = stds[i].year;
	return 1;
      }
  return 0;
}

static void
set_warning (struct options *o, const char *name, int on)
{
  if (strcmp (name, "all") == 0)
    {
      o->warn_comments = on;
      o->warn_trigraphs = on ? 1 : o->warn_trigraphs;
      return;
    }
  if (strcmp (name, "extra") == 0)
    {
      o->warn_expansion_to_defined = on;
      return;
    }
  int r = warning_reason (name);
  if (r > 0 && r < 64)
    warning_off[r] = !on;
  if (strcmp (name, "comment") == 0 || strcmp (name, "comments") == 0) o->warn_comments = on;
  else if (strcmp (name, "undef") == 0) o->warn_undef = on;
  else if (strcmp (name, "unused-macros") == 0) o->warn_unused_macros = on;
  else if (strcmp (name, "long-long") == 0) o->warn_long_long = on;
  else if (strcmp (name, "endif-labels") == 0) o->warn_endif_labels = on;
  else if (strcmp (name, "trigraphs") == 0) o->warn_trigraphs = on;
  else if (strcmp (name, "traditional") == 0) o->warn_traditional = on;
  else if (strcmp (name, "expansion-to-defined") == 0) o->warn_expansion_to_defined = on;
  else if (strcmp (name, "date-time") == 0) o->warn_date_time = on;
  else if (strcmp (name, "multichar") == 0) o->warn_multichar = on;
  else if (strcmp (name, "invalid-utf8") == 0) o->warn_invalid_utf8 = on;
  else if (strcmp (name, "deprecated") == 0) o->warn_deprecated = on;
  else if (strcmp (name, "literal-suffix") == 0) o->warn_literal_suffix = on;
  else if (strcmp (name, "variadic-macros") == 0) o->warn_variadic = on;
  else if (strcmp (name, "builtin-macro-redefined") == 0) o->warn_builtin_redefined = on;
  else if (strcmp (name, "normalized") == 0) o->warn_normalize = on ? normalized_C : normalized_none;
  else if (strcmp (name, "header-guard") == 0) o->warn_header_guard = on;
  else if (strcmp (name, "c++11-compat") == 0 || strcmp (name, "c++0x-compat") == 0) o->warn_cxx11_compat = on;
  else if (strcmp (name, "c90-c99-compat") == 0) o->warn_c90_c99 = on;
  else if (strcmp (name, "c11-c23-compat") == 0) o->warn_c11_c23 = on;
  else if (strcmp (name, "unicode") == 0) o->warn_unicode = on;
  else if (strcmp (name, "bidi-chars") == 0) o->warn_bidi = on ? bidirectional_unpaired : bidirectional_none;
  else if (strcmp (name, "missing-include-dirs") == 0) o->warn_missing_include_dirs = on;
}

/* the predefined macros' file (--predef, before the options or a list's line's own), read once while it stays the same */
static void
set_predef (const char *path)
{
  if (predef_file && strcmp (predef_file, path) == 0 && predef_text)
    return;
  free (predef_text);
  predef_file = xstrdup (path);
  predef_text = read_all (path, NULL);
  if (!predef_text)
    {
      fprintf (stderr, "ppref: cannot read %s\n", path);
      exit (2);
    }
}

/* the options in order, cc1plus's way: the last -std= wins; an option's argument in the next word or joined */
static const char *
parse_options (struct options *o, int argc, char **argv)
{
  const char *file = NULL;
  memset (o, 0, sizeof *o);
  o->cxx = lang_cxx;
  o->lang = lang_cxx ? CLK_GNUCXX17 : CLK_GNUC23;
  o->year = lang_cxx ? 2017 : 2023;
  o->ext_identifiers = 1;
  o->dollars = 1;
  o->ext_numeric = -1;
  o->operator_names = 1;
  o->warn_endif_labels = 1;
  o->warn_trigraphs = 2;
  o->warn_multichar = 1;
  o->warn_deprecated = 1;
  o->warn_literal_suffix = 1;
  o->warn_variadic = 1;
  o->warn_builtin_redefined = 1;
  o->warn_normalize = normalized_C;
  o->warn_unicode = 1;
  o->warn_bidi = bidirectional_unpaired;
  o->warn_long_long = -1;
  o->warn_c90_c99 = -1;
  o->warn_c11_c23 = -1;
  o->input_charset = "UTF-8";
  memset (werror_on, 0, sizeof werror_on);
  memset (werror_off, 0, sizeof werror_off);
  memset (warning_off, 0, sizeof warning_off);
  pedantic_errors = werror = no_warnings = wsystem_headers = 0;
  for (int i = 0; i < argc; i++)
    {
      const char *a = argv[i];
      const char *next = i + 1 < argc ? argv[i + 1] : NULL;
#define JOINED(flag) (strncmp (a, flag, strlen (flag)) == 0)
#define ARG(flag) (a[strlen (flag)] ? a + strlen (flag) : (i++, next))
      if (strcmp (a, "--predef") == 0 && next)
	{
	  set_predef (next);
	  i++;
	}
      else if (a[0] != '-' || a[1] == '\0')
	file = a;
      else if (JOINED ("-std="))
	set_std (o, a + 5);
      else if (strcmp (a, "-ansi") == 0)
	set_std (o, o->cxx ? "c++98" : "c90");
      else if (strcmp (a, "-x") == 0)
	{
	  /* the driver's language: assembler-with-cpp is cpp's CLK_ASM (c-opts.cc's OPT_lang_asm) */
	  if (next && strcmp (next, "assembler-with-cpp") == 0)
	    o->asm_cpp = 1;
	  i++;
	}
      else if (JOINED ("-iquote"))
	{
	  const char *d = ARG ("-iquote");
	  if (d) push_dir (&o->quote, &o->n_quote, d, 0);
	}
      else if (JOINED ("-isystem"))
	{
	  const char *d = ARG ("-isystem");
	  if (d) push_dir (&o->isystem, &o->n_isystem, d, 1);
	}
      else if (JOINED ("-idirafter"))
	{
	  const char *d = ARG ("-idirafter");
	  if (d) push_dir (&o->after, &o->n_after, d, 1);
	}
      else if (JOINED ("--embed-dir") || JOINED ("-embed-dir"))
	{
	  /* c-opts.cc's OPT__embed_dir_: --embed-dir=DIR (and its other spellings) */
	  const char *eq = strchr (a, '=');
	  const char *d = eq ? eq + 1 : (i++, next);
	  if (d) push_dir (&o->embed, &o->n_embed, d, 0);
	}
      else if (JOINED ("-include"))
	{
	  const char *f = ARG ("-include");
	  if (f)
	    {
	      o->includes = (const char **) xrealloc (o->includes, (o->n_includes + 1) * sizeof (char *));
	      o->includes[o->n_includes++] = f;
	    }
	}
      else if (JOINED ("-imacros") || JOINED ("-isysroot") || JOINED ("-iprefix") || JOINED ("-iwithprefix")
	       || strcmp (a, "--param") == 0 || strcmp (a, "-B") == 0)
	{
	  if (strcmp (a, "-imacros") == 0 || strcmp (a, "-isysroot") == 0 || strcmp (a, "-iprefix") == 0
	      || strcmp (a, "-iwithprefix") == 0 || strcmp (a, "-iwithprefixbefore") == 0 || strcmp (a, "--param") == 0
	      || strcmp (a, "-B") == 0)
	    i++;
	}
      else if (JOINED ("-I"))
	{
	  const char *d = ARG ("-I");
	  if (d && strcmp (d, "-") != 0) push_dir (&o->bracket, &o->n_bracket, d, 0);
	}
      else if (JOINED ("-D") || JOINED ("-U"))
	{
	  if (a[2] == '\0')
	    i++;
	}
      else if (strcmp (a, "-nostdinc") == 0)
	o->nostdinc = 1;
      else if (strcmp (a, "-nostdinc++") == 0)
	o->nostdincxx = 1;
      else if (strcmp (a, "-trigraphs") == 0)
	o->trigraphs = 1;
      else if (strcmp (a, "-pedantic") == 0 || strcmp (a, "-Wpedantic") == 0)
	o->pedantic = 1;
      else if (strcmp (a, "-pedantic-errors") == 0)
	{
	  o->pedantic = 1;
	  pedantic_errors = 1;
	}
      else if (strcmp (a, "-Wno-pedantic") == 0)
	{
	  o->pedantic = 0;
	  warning_off[CPP_W_PEDANTIC] = 1;
	}
      else if (strcmp (a, "-w") == 0)
	no_warnings = 1;
      else if (strcmp (a, "-Werror") == 0)
	werror = 1;
      else if (strcmp (a, "-Wno-error") == 0)
	werror = 0;
      else if (JOINED ("-Werror="))
	{
	  int r = warning_reason (a + 8);
	  if (r > 0 && r < 64)
	    {
	      werror_on[r] = 1;
	      warning_off[r] = 0;
	    }
	  if (strcmp (a + 8, "pedantic") == 0)
	    o->pedantic = 1;
	  set_warning (o, a + 8, 1);
	}
      else if (JOINED ("-Wno-error="))
	{
	  int r = warning_reason (a + 11);
	  if (r > 0 && r < 64)
	    {
	      werror_off[r] = 1;
	      werror_on[r] = 0;
	    }
	}
      else if (strcmp (a, "-Wsystem-headers") == 0)
	wsystem_headers = 1;
      else if (JOINED ("-Wno-"))
	set_warning (o, a + 5, 0);
      else if (JOINED ("-W") && !JOINED ("-Wl,") && !JOINED ("-Wa,") && !JOINED ("-Wp,"))
	{
	  char *n = xstrdup (a + 2);
	  char *eq = strchr (n, '=');
	  if (eq)
	    *eq = '\0';
	  set_warning (o, n, 1);
	  free (n);
	}
      else if (strcmp (a, "-fextended-identifiers") == 0)
	o->ext_identifiers = 1;
      else if (strcmp (a, "-fno-extended-identifiers") == 0)
	o->ext_identifiers = 0;
      else if (strcmp (a, "-fdollars-in-identifiers") == 0)
	o->dollars = 1;
      else if (strcmp (a, "-fno-dollars-in-identifiers") == 0)
	o->dollars = 0;
      else if (strcmp (a, "-fext-numeric-literals") == 0)
	o->ext_numeric = 1;
      else if (strcmp (a, "-fno-ext-numeric-literals") == 0)
	o->ext_numeric = 0;
      else if (strcmp (a, "-foperator-names") == 0)
	o->operator_names = 1;
      else if (strcmp (a, "-fno-operator-names") == 0)
	o->operator_names = 0;
      else if (strcmp (a, "-funsigned-char") == 0 || strcmp (a, "-fno-signed-char") == 0)
	o->unsigned_char = 1;
      else if (strcmp (a, "-fsigned-char") == 0 || strcmp (a, "-fno-unsigned-char") == 0)
	o->unsigned_char = 0;
      else if (JOINED ("-finput-charset="))
	o->input_charset = a + 16;
      else if (JOINED ("-fexec-charset="))
	o->narrow_charset = a + 15;
      else if (JOINED ("-fwide-exec-charset="))
	o->wide_charset = a + 20;
      else if (strcmp (a, "-o") == 0 || strcmp (a, "-MF") == 0 || strcmp (a, "-MT") == 0 || strcmp (a, "-MQ") == 0
	       || strcmp (a, "-Xpreprocessor") == 0 || strcmp (a, "-Xassembler") == 0 || strcmp (a, "-Xlinker") == 0
	       || strcmp (a, "-aux-info") == 0 || strcmp (a, "-L") == 0 || strcmp (a, "-l") == 0)
	i++;
#undef JOINED
#undef ARG
    }
  /* gcc's driver reads a .S file as assembler-with-cpp */
  if (file && !o->cxx)
    {
      size_t n = strlen (file);
      if (n > 2 && strcmp (file + n - 2, ".S") == 0)
	o->asm_cpp = 1;
    }
  if (o->asm_cpp)
    {
      o->lang = CLK_ASM;
      o->dollars = 0;
    }
  return file;
}

static cpp_dir *
make_dir (const char *name, int sysp, int under_sysroot)
{
  cpp_dir *d = (cpp_dir *) xalloc0 (sizeof (cpp_dir));
  if (under_sysroot && sysroot)
    {
      d->name = (char *) xmalloc (sysroot_len + strlen (name) + 1);
      strcpy (d->name, sysroot);
      strcat (d->name, name);
    }
  else
    d->name = xstrdup (name);
  size_t len = strlen (d->name);
  while (len > 1 && d->name[len - 1] == '/')
    d->name[--len] = '\0';
  d->len = len;
  d->sysp = sysp;
  d->user_supplied_p = !under_sysroot;
  return d;
}

/* the search chains, incpath.cc's order: -iquote, then -I, -isystem, the standard directories, -idirafter */
static void
set_chains (cpp_reader *pfile, struct options *o)
{
  cpp_dir *head = NULL, *tail = NULL, *quote_head = NULL, *quote_tail = NULL;
#define ADD(H, T, D)                                                                                                     \
  do                                                                                                                     \
    {                                                                                                                    \
      cpp_dir *d_ = (D);                                                                                                 \
      if (T)                                                                                                             \
	(T)->next = d_;                                                                                                  \
      else                                                                                                               \
	(H) = d_;                                                                                                        \
      (T) = d_;                                                                                                          \
    }                                                                                                                    \
  while (0)
  for (int i = 0; i < o->n_quote; i++)
    ADD (quote_head, quote_tail, make_dir (o->quote[i].name, 0, 0));
  for (int i = 0; i < o->n_bracket; i++)
    ADD (head, tail, make_dir (o->bracket[i].name, 0, 0));
  for (int i = 0; i < o->n_isystem; i++)
    ADD (head, tail, make_dir (o->isystem[i].name, 1, 0));
  if (!o->nostdinc)
    for (int i = 0; i < n_std_dirs; i++)
      {
	if (o->nostdincxx && strstr (std_dirs[i].name, "/include/c++/"))
	  continue;
	ADD (head, tail, make_dir (std_dirs[i].name, std_dirs[i].sysp, 1));
      }
  for (int i = 0; i < o->n_after; i++)
    ADD (head, tail, make_dir (o->after[i].name, 1, 0));
  if (quote_tail)
    quote_tail->next = head;
  else
    quote_head = head;
  cpp_dir *embed_head = NULL, *embed_tail = NULL;
  for (int i = 0; i < o->n_embed; i++)
    ADD (embed_head, embed_tail, make_dir (o->embed[i].name, 0, 0));
  cpp_set_include_chains (pfile, quote_head, head, embed_head, 0);
#undef ADD
}

/* the target's predefined macros (#define NAME BODY, #undef NAME), each as cpp_define's NAME=BODY: libcpp's builtins left as they
   are, a macro libcpp defined already with the same body left too */
static void
define_predefined (cpp_reader *pfile)
{
  if (!predef_text)
    return;
  char *text = xstrdup (predef_text);
  char *p = text;
  while (*p)
    {
      char *nl = strchr (p, '\n');
      if (nl)
	*nl = '\0';
      if (strncmp (p, "#define ", 8) == 0)
	{
	  char *name = p + 8;
	  char *e = name;
	  while (*e && *e != ' ' && *e != '(')
	    e++;
	  if (*e == '(')
	    {
	      while (*e && *e != ')')
		e++;
	      if (*e)
		e++;
	    }
	  char saved = *e;
	  *e = '\0';
	  const char *body = saved ? e + 1 : "";
	  char *ident = xstrdup (name);
	  char *paren = strchr (ident, '(');
	  if (paren)
	    *paren = '\0';
	  cpp_hashnode *node = cpp_lookup (pfile, (const unsigned char *) ident, strlen (ident));
	  int skip = 0;
	  if (node->type == NT_BUILTIN_MACRO)
	    skip = 1;
	  else if (cpp_macro_p (node))
	    {
	      const unsigned char *def = cpp_macro_definition (pfile, node);
	      char *want = (char *) xmalloc (strlen (name) + strlen (body) + 2);
	      sprintf (want, "%s %s", name, body);
	      if (strcmp ((const char *) def, want) == 0 || (!*body && strcmp ((const char *) def, name) == 0))
		skip = 1;
	      else
		cpp_undef (pfile, ident);
	      free (want);
	    }
	  if (!skip)
	    {
	      char *d = (char *) xmalloc (strlen (name) + strlen (body) + 2);
	      sprintf (d, "%s=%s", name, body);
	      cpp_define (pfile, d);
	      free (d);
	    }
	  free (ident);
	  *e = saved;
	}
      else if (strncmp (p, "#undef ", 7) == 0)
	{
	  char *name = p + 7;
	  cpp_hashnode *node = cpp_lookup (pfile, (const unsigned char *) name, strlen (name));
	  if (node->type != NT_BUILTIN_MACRO && cpp_macro_p (node))
	    cpp_undef (pfile, name);
	}
      if (!nl)
	break;
      p = nl + 1;
    }
  free (text);
}

static void
apply_options (cpp_reader *pfile, struct options *o)
{
  cpp_options *co = cpp_get_options (pfile);
  if (o->trigraphs)
    co->trigraphs = 1;
  co->extended_identifiers = o->ext_identifiers;
  co->dollars_in_ident = o->dollars;
  co->operator_names = o->operator_names;
  co->cpp_pedantic = o->pedantic;
  if (o->pedantic)
    co->warn_endif_labels = 1;
  co->warn_dollars = 0;
  co->warn_comments = o->warn_comments;
  co->warn_undef = o->warn_undef;
  co->warn_unused_macros = 0;
  co->warn_endif_labels = o->warn_endif_labels || o->pedantic;
  co->warn_trigraphs = o->warn_trigraphs;
  co->cpp_warn_traditional = o->warn_traditional;
  co->warn_expansion_to_defined = o->warn_expansion_to_defined;
  co->warn_date_time = o->warn_date_time;
  co->warn_multichar = o->warn_multichar;
  co->cpp_warn_invalid_utf8 = o->warn_invalid_utf8;
  co->cpp_warn_deprecated = o->warn_deprecated;
  co->warn_literal_suffix = o->warn_literal_suffix;
  co->warn_variadic_macros = o->warn_variadic;
  co->warn_builtin_macro_redefined = o->warn_builtin_redefined;
  co->warn_normalize = o->warn_normalize;
  co->warn_header_guard = o->warn_header_guard;
  co->cpp_warn_unicode = o->warn_unicode;
  co->cpp_warn_bidirectional = o->warn_bidi;
  co->warn_missing_include_dirs = o->warn_missing_include_dirs;
  co->cpp_warn_c90_c99_compat = o->warn_c90_c99;
  co->cpp_warn_c11_c23_compat = o->warn_c11_c23;
  co->cpp_warn_long_long = o->warn_long_long == -1 ? (o->pedantic && (o->cxx ? o->year == 1998 : o->year < 1999))
						   : o->warn_long_long;
  if (o->cxx)
    {
      co->cpp_warn_cxx11_compat = o->year >= 2011 ? 0 : o->warn_cxx11_compat;
      if (o->year >= 2020)
	co->cpp_warn_cxx20_compat = 0;
      if (o->ext_numeric != -1)
	co->ext_numeric_literals = o->ext_numeric;
      else if (o->year >= 2011 && o->iso)
	co->ext_numeric_literals = 0;
    }
  co->input_charset = o->input_charset;
  if (o->narrow_charset)
    co->narrow_charset = o->narrow_charset;
  if (o->wide_charset)
    co->wide_charset = o->wide_charset;
  /* x86_64-linux: c-family's from the target's types (c_common_init) */
  co->precision = 64;
  co->char_precision = 8;
  co->int_precision = 32;
  co->wchar_precision = 32;
  co->unsigned_wchar = 0;
  co->unsigned_char = o->unsigned_char;
  co->unsigned_utf8char = 1;
  co->bytes_big_endian = 0;
  co->cpp_tabstop = 8;
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* Output */

static void
record (int kind, const char *text, size_t len)
{
  fprintf (out, "%c%lu ", kind, (unsigned long) len);
  fwrite (text, 1, len, out);
  fputc ('\n', out);
}

static unsigned char *spell_buf;
static size_t spell_cap;

static size_t
spell (cpp_reader *pfile, const cpp_token *t)
{
  size_t need = cpp_token_len (t) + 16;
  if (need > spell_cap)
    {
      spell_cap = need * 2;
      spell_buf = (unsigned char *) xrealloc (spell_buf, spell_cap);
    }
  unsigned char *end = cpp_spell_token (pfile, t, spell_buf, false);
  return (size_t) (end - spell_buf);
}

static void
print_stream (cpp_reader *pfile)
{
  int in_pragma = 0;
  for (;;)
    {
      const cpp_token *t = cpp_get_token (pfile);
      if (t->type == CPP_EOF)
	break;
      if (fatal)
	break;
      if (t->type == CPP_PADDING)
	continue;
      if (t->type == CPP_PRAGMA)
	{
	  in_pragma = 1;
	  continue;
	}
      if (t->type == CPP_PRAGMA_EOL)
	{
	  in_pragma = 0;
	  continue;
	}
      if (in_pragma)
	continue;
      if (t->type == CPP_EMBED)
	{
	  char num[8];
	  for (unsigned i = 0; i < t->val.str.len; i++)
	    {
	      if (i)
		record ('t', ",", 1);
	      int n = sprintf (num, "%u", (unsigned) t->val.str.text[i]);
	      record ('t', num, (size_t) n);
	    }
	  continue;
	}
      size_t len = spell (pfile, t);
      if (t->type == CPP_NAME)
	record ('i', (const char *) spell_buf, len);
      else if (cpp_userdef_string_p (t->type) || cpp_userdef_char_p (t->type))
	{
	  const char *suffix = cpp_get_userdef_suffix (t);
	  size_t lit = (size_t) (suffix - (const char *) t->val.str.text);
	  record ('t', (const char *) t->val.str.text, lit);
	  record ('i', suffix, t->val.str.len - lit);
	}
      else
	record ('t', (const char *) spell_buf, len);
    }
}

/* the raw tokens with their places in the file's bytes */
static void
print_raw (cpp_reader *pfile, const char *path)
{
  size_t n;
  char *bytes = read_all (path, &n);
  if (!bytes)
    return;
  size_t nlines = 1;
  for (size_t i = 0; i < n; i++)
    if (bytes[i] == '\n')
      nlines++;
  size_t *starts = (size_t *) xmalloc ((nlines + 2) * sizeof (size_t));
  size_t k = 0;
  starts[k++] = 0;
  for (size_t i = 0; i < n; i++)
    if (bytes[i] == '\n')
      starts[k++] = i + 1;
  starts[k] = n;
  char *text = (char *) xmalloc (n + 1);
  char head[64];
  pfile->state.parsing_args = 1;
  for (;;)
    {
      const cpp_token *t = _cpp_lex_token (pfile);
      if (t->type == CPP_EOF)
	break;
      if (t->type == CPP_PADDING || t->type == CPP_COMMENT)
	continue;
      source_range r = get_range_from_loc (lt, t->src_loc);
      expanded_location s = linemap_client_expand_location_to_spelling_point (lt, r.m_start, LOCATION_ASPECT_START);
      expanded_location e = linemap_client_expand_location_to_spelling_point (lt, r.m_finish, LOCATION_ASPECT_FINISH);
      if (s.line < 1 || (size_t) s.line > nlines || e.line < 1 || (size_t) e.line > nlines || s.column < 1)
	{
	  record ('l', "#noplace", 8);
	  continue;
	}
      size_t so = starts[s.line - 1] + (size_t) s.column - 1;
      size_t eo = starts[e.line - 1] + (size_t) e.column;  /* exclusive: the column of the last byte, 1-based */
      if (e.column < 1)
	eo = starts[e.line - 1];
      if (eo > n)
	eo = n;
      if (so > eo)
	eo = so;
      /* the token placed at the splices that touch it from the left */
      size_t po = so;
      for (;;)
	{
	  if (po >= 2 && bytes[po - 1] == '\n' && bytes[po - 2] == '\\')
	    po -= 2;
	  else if (po >= 3 && bytes[po - 1] == '\n' && bytes[po - 2] == '\r' && bytes[po - 3] == '\\')
	    po -= 3;
	  else
	    break;
	}
      size_t line = 1, lo = 0, hi = k;
      while (lo + 1 < hi)
	{
	  size_t mid = (lo + hi) / 2;
	  if (starts[mid] <= po)
	    lo = mid;
	  else
	    hi = mid;
	}
      line = lo + 1;
      size_t col = po - starts[lo] + 1;
      /* the text without its splices */
      size_t m = 0;
      for (size_t i = so; i < eo; i++)
	{
	  if (bytes[i] == '\\' && i + 1 < eo && bytes[i + 1] == '\n')
	    {
	      i++;
	      continue;
	    }
	  if (bytes[i] == '\\' && i + 2 < eo && bytes[i + 1] == '\r' && bytes[i + 2] == '\n')
	    {
	      i += 2;
	      continue;
	    }
	  text[m++] = bytes[i];
	}
      int hl = sprintf (head, "%lu:%lu\t", (unsigned long) line, (unsigned long) col);
      size_t total = (size_t) hl + m;
      fprintf (out, "l%lu ", (unsigned long) total);
      fwrite (head, 1, (size_t) hl, out);
      fwrite (text, 1, m, out);
      fputc ('\n', out);
    }
  free (text);
  free (starts);
  free (bytes);
}

/* ------------------------------------------------------------------------------------------------------------------------ */
/* One file */

static int
run_one (int raw, int argc, char **argv, FILE *dest)
{
  struct options o;
  const char *file = parse_options (&o, argc, argv);
  out = dest;
  errors = 0;
  fatal = 0;
  quiet = raw;
  if (!file)
    {
      fprintf (stderr, "ppref: no file\n");
      return 2;
    }
  lt = (line_maps *) xalloc0 (sizeof (line_maps));
  linemap_init (lt, BUILTINS_LOCATION);
  lt->m_reallocator = xrealloc;
  lt->m_round_alloc_size = round_alloc_size;
  cpp_reader *pfile = cpp_create_reader (o.lang, NULL, lt);
  reader = pfile;
  cpp_options *co = cpp_get_options (pfile);
  co->warn_dollars = 0;
  apply_options (pfile, &o);
  cpp_callbacks *cb = cpp_get_callbacks (pfile);
  cb->diagnostic = diagnostic;
  cb->has_attribute = has_attribute;
  cb->has_builtin = has_builtin;
  cb->has_feature = has_feature;
  cb->def_pragma = def_pragma;
  cb->remap_filename = remap_filename;
  if (epoch >= 0)
    cb->get_source_date_epoch = source_date_epoch;
  set_chains (pfile, &o);
  cpp_post_options (pfile);
  /* c_common_init's: the conversions once the target's precisions are known */
  cpp_init_iconv (pfile);
  const char *name = cpp_read_main_file (pfile, file, !raw);
  if (!name)
    {
      fflush (out);
      return 1;
    }
  if (raw)
    {
      print_raw (pfile, file);
    }
  else
    {
      /* c-opts.cc's c_finish_options: the builtins and the target's macros in <built-in>, the command line's in <command-line>,
	 -include's files, then the main file */
      linemap_add (lt, LC_RENAME, 0, "<built-in>", 0);
      linemap_line_start (lt, 0, 1);
      cpp_force_token_locations (pfile, BUILTINS_LOCATION);
      cpp_init_builtins (pfile, 1);
      define_predefined (pfile);
      co->warn_dollars = (co->cpp_pedantic && !co->c99);
      linemap_add (lt, LC_RENAME, 0, "<command-line>", 0);
      linemap_line_start (lt, 0, 1);
      cpp_stop_forcing_token_locations (pfile);
      for (int i = o.n_includes - 1; i >= 0; i--)
	cpp_push_include (pfile, o.includes[i]);
      if (o.n_includes == 0)
	cpp_change_file (pfile, LC_RENAME, file);
      print_stream (pfile);
    }
  fflush (out);
  if (!fatal && !raw)
    cpp_finish (pfile, NULL);
  /* no cpp_destroy: cc1 never calls it, and its file cleanup frees the names __has_include gave (tbb/tbb.h): a list's
     process keeps each file's reader, so the check gives a process a few files */
  return errors ? 1 : 0;
}

/* ------------------------------------------------------------------------------------------------------------------------ */

static void
usage (void)
{
  fprintf (stderr, "usage: ppref pp|lex [--lang c|c++] [--predef F] [--has F] [--sys D]... [--sysroot D] [--epoch N] "
		   "[--list F] [--] OPTION... FILE\n");
  exit (2);
}

int
main (int argc, char **argv)
{
  if (argc < 2)
    usage ();
  int raw;
  if (strcmp (argv[1], "pp") == 0)
    raw = 0;
  else if (strcmp (argv[1], "lex") == 0)
    raw = 1;
  else
    usage ();
  const char *list = NULL;
  int i = 2;
  for (; i < argc; i++)
    {
      const char *a = argv[i];
      const char *v = i + 1 < argc ? argv[i + 1] : NULL;
      if (strcmp (a, "--") == 0)
	{
	  i++;
	  break;
	}
      else if (strcmp (a, "--lang") == 0 && v)
	{
	  lang_cxx = strcmp (v, "c") != 0;
	  i++;
	}
      else if (strcmp (a, "--predef") == 0 && v)
	{
	  predef_file = v;
	  i++;
	}
      else if (strcmp (a, "--has") == 0 && v)
	{
	  has_file = v;
	  i++;
	}
      else if (strcmp (a, "--sys") == 0 && v)
	{
	  std_dirs = (struct dir_row *) xrealloc (std_dirs, (n_std_dirs + 1) * sizeof (struct dir_row));
	  std_dirs[n_std_dirs].name = xstrdup (v);
	  std_dirs[n_std_dirs].sysp = 1;
	  n_std_dirs++;
	  i++;
	}
      else if (strcmp (a, "--sysroot") == 0 && v)
	{
	  sysroot = v;
	  sysroot_len = strlen (v);
	  while (sysroot_len > 1 && sysroot[sysroot_len - 1] == '/')
	    sysroot_len--;
	  i++;
	}
      else if (strcmp (a, "--epoch") == 0 && v)
	{
	  epoch = strtoll (v, NULL, 10);
	  i++;
	}
      else if (strcmp (a, "--list") == 0 && v)
	{
	  list = v;
	  i++;
	}
      else
	break;
    }
  if (has_file)
    load_has (has_file);
  if (predef_file)
    {
      const char *p = predef_file;
      predef_file = NULL;
      set_predef (p);
    }
  errout = stderr;
  if (!list)
    return run_one (raw, argc - i, argv + i, stdout);
  /* the list: OUT<tab>ARG... a line */
  size_t len;
  char *text = read_all (list, &len);
  if (!text)
    {
      fprintf (stderr, "ppref: cannot read %s\n", list);
      return 2;
    }
  char *p = text;
  while (*p)
    {
      char *nl = strchr (p, '\n');
      if (nl)
	*nl = '\0';
      if (*p)
	{
	  int n = 1;
	  for (char *q = p; *q; q++)
	    if (*q == '\t')
	      n++;
	  char **fields = (char **) xmalloc ((n + 1) * sizeof (char *));
	  int f = 0;
	  fields[f++] = p;
	  for (char *q = p; *q; q++)
	    if (*q == '\t')
	      {
		*q = '\0';
		fields[f++] = q + 1;
	      }
	  FILE *dest = fopen (fields[0], "wb");
	  if (!dest)
	    {
	      fprintf (stderr, "ppref: cannot write %s\n", fields[0]);
	      return 2;
	    }
	  char *err = (char *) xmalloc (strlen (fields[0]) + 8);
	  sprintf (err, "%s.err", fields[0]);
	  FILE *ef = fopen (err, "wb");
	  errout = ef ? ef : stderr;
	  int st = run_one (raw, f - 1, fields + 1, dest);
	  fclose (dest);
	  if (ef)
	    fclose (ef);
	  errout = stderr;
	  sprintf (err, "%s.status", fields[0]);
	  FILE *sf = fopen (err, "wb");
	  if (sf)
	    {
	      fprintf (sf, "%d\n", st);
	      fclose (sf);
	    }
	  free (err);
	  free (fields);
	}
      if (!nl)
	break;
      p = nl + 1;
    }
  return 0;
}

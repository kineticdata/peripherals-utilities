Utilities [handlers] (2020-10-16)
* [utility_json_path_parse_v1]
    * initial commit.
* [utility_file_append_v1]
    * initial commit.

Utilities [file-adapters] (2021-03-01)
* [kinetic-filehub-adapter-local]
    * updated adapter response to a file not found error.


Utilities [handlers] (2021-09-03)
* [utility_csv_to_json_v1]
    * Fixed bug with standard error

Utilities [handlers] (2022-04-14)
* [generic_file_upload_ce_attachment_v1]
    * Initial commit of handler 

Utilities [bridge-adapter] (2022-07-25)
* [kinetic-bridgehub-adapter-generic-rest]
    * Added XML as a source schema option

Utilities [bridge-adapters] (2024-02-20)
  * [kinetic-bridgehub-adapter-generic-rest] v1.0.2
    * updated snakeyaml version due to vulnerability
    * updated org.json version due to vulnerability

Utilities [handlers] (2026-10-20)
* [utility_determine_next_occurrence_v2]
    * Upgraded IceCube to 0.16.4 so the handler runs on JRuby 10 (Fixnum was removed in Ruby 3.2)
    * Fixed input validation, weekly schedules with a Timing value, and schedules that can never occur hanging the engine

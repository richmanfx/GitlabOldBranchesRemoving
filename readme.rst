GitlabOldBranchesRemoving
=========================

Russian_ / English_

.. |rus-flag| image:: resource/rus-flag.png
.. |eng-flag| image:: resource/eng-flag.png

----------


.. _Russian:

|rus-flag|

Что это?
--------
Удаляет старые смерженные ветки, начиная с даты "срока давности".
Если дата "срока давности" не была передана, то считаем ее равной дате, которая была месяц тому назад.
Манипуляции производятся относително "главной ветки" - **develop**, можно в скрипте заменить, например,
на ветку **master**.

Как использовать?
-----------------
groovy GitlabOldBranchesRm ~/IdeaProjects/name_of_your_project 01-01-2019

.. _English:


|eng-flag|

What is it?
-----------
Removes old merged branches, starting from the date of "limitation period".
If the date of "limitation period" has not been transferred, we consider it equal to the date that was a month ago.
Manipulations are performed with respect to the "main branch" - **develop**, you can replace, for example,
the **master** branch in the script.

How to use?
-----------
groovy GitlabOldBranchesRm ~/IdeaProjects/name_of_your_project 01-01-2019